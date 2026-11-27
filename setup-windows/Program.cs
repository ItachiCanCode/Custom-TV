using System.Diagnostics;
using System.IO.Compression;
using System.Text;

namespace VoidLauncherSetup;

static class Program
{
    [STAThread]
    static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}

/// <summary>
/// Windows companion: downloads adb + the latest VoidLauncher APK and installs it
/// on an Android TV over the network (ADB over TCP).
/// </summary>
class MainForm : Form
{
    const string ApkUrl = "https://github.com/ItachiCanCode/Custom-TV/releases/latest/download/VoidLauncher.apk";
    const string AdbZipUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip";
    const string LauncherActivity = "io.github.itachicancode.voidlauncher/.MainActivity";

    readonly TextBox ipBox = new() { PlaceholderText = "TV IP address, e.g. 192.168.1.50", Width = 260 };
    readonly TextBox apkBox = new() { PlaceholderText = "Optional: path to a local APK (leave empty to download latest)", Dock = DockStyle.Fill };
    readonly Button browseBtn = new() { Text = "Browse...", AutoSize = true };
    readonly Button installBtn = new() { Text = "Install launcher on TV", AutoSize = true };
    readonly TextBox logBox = new() { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill };
    readonly string workDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "VoidLauncherSetup");

    public MainForm()
    {
        Text = "Void Launcher Setup";
        try { Icon = Icon.ExtractAssociatedIcon(Environment.ProcessPath!); } catch { /* keep default window icon */ }
        Width = 720; Height = 520;
        StartPosition = FormStartPosition.CenterScreen;

        var help = new Label
        {
            Dock = DockStyle.Top, Height = 70, Padding = new Padding(8),
            Text = "On the TV: Settings > Device Preferences > About > click Build 7 times, then Developer options > " +
                   "enable USB/Network debugging. PC and TV must be on the same network. Enter the TV's IP below and " +
                   "accept the debugging prompt on the TV."
        };
        var row1 = new FlowLayoutPanel { Dock = DockStyle.Top, Height = 40, Padding = new Padding(8, 4, 8, 4) };
        row1.Controls.Add(new Label { Text = "TV IP:", AutoSize = true, Padding = new Padding(0, 6, 0, 0) });
        row1.Controls.Add(ipBox);
        row1.Controls.Add(installBtn);
        var row2 = new TableLayoutPanel { Dock = DockStyle.Top, Height = 36, ColumnCount = 3, Padding = new Padding(8, 2, 8, 2) };
        row2.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        row2.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        row2.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        row2.Controls.Add(new Label { Text = "APK:", AutoSize = true, Padding = new Padding(0, 6, 0, 0) }, 0, 0);
        row2.Controls.Add(apkBox, 1, 0);
        row2.Controls.Add(browseBtn, 2, 0);

        Controls.Add(logBox);
        Controls.Add(row2);
        Controls.Add(row1);
        Controls.Add(help);

        browseBtn.Click += (_, _) =>
        {
            using var dlg = new OpenFileDialog { Filter = "APK files (*.apk)|*.apk" };
            if (dlg.ShowDialog(this) == DialogResult.OK) apkBox.Text = dlg.FileName;
        };
        installBtn.Click += async (_, _) => await RunInstallAsync();
    }

    void Log(string msg)
    {
        if (InvokeRequired) { BeginInvoke(() => Log(msg)); return; }
        logBox.AppendText(msg + Environment.NewLine);
    }

    async Task RunInstallAsync()
    {
        var target = ipBox.Text.Trim();
        if (target.Length == 0) { Log("Enter the TV IP address first."); return; }
        if (!target.Contains(':')) target += ":5555";
        installBtn.Enabled = false;
        try
        {
            Directory.CreateDirectory(workDir);
            var adb = await EnsureAdbAsync();
            var apk = await EnsureApkAsync();

            Log($"Connecting to {target} (accept the prompt on the TV)...");
            await Adb(adb, "disconnect " + target);
            await Adb(adb, "connect " + target);
            Log("Installing...");
            var rc = await Adb(adb, $"-s {target} install -r \"{apk}\"");
            if (rc != 0) { Log("Install failed. Check that debugging is enabled and the TV accepted this PC."); return; }
            Log("Installed. Opening the home chooser on the TV...");
            await Adb(adb, $"-s {target} shell cmd package set-home-activity {LauncherActivity}");
            await Adb(adb, $"-s {target} shell am start -a android.intent.action.MAIN -c android.intent.category.HOME");
            Log("Done! On the TV choose \"Void Launcher\" as the home app if asked.");
        }
        catch (Exception ex) { Log("Error: " + ex.Message); }
        finally { installBtn.Enabled = true; }
    }

    async Task<string> EnsureAdbAsync()
    {
        var adb = Path.Combine(workDir, "platform-tools", "adb.exe");
        if (File.Exists(adb)) return adb;
        Log("Downloading adb (Android platform-tools)...");
        var zip = Path.Combine(workDir, "platform-tools.zip");
        await DownloadAsync(AdbZipUrl, zip);
        ZipFile.ExtractToDirectory(zip, workDir, true);
        File.Delete(zip);
        if (!File.Exists(adb)) throw new FileNotFoundException("adb.exe not found after extraction.");
        return adb;
    }

    async Task<string> EnsureApkAsync()
    {
        var local = apkBox.Text.Trim().Trim('"');
        if (local.Length > 0)
        {
            if (!File.Exists(local)) throw new FileNotFoundException("APK not found: " + local);
            return local;
        }
        Log("Downloading latest launcher APK...");
        var apk = Path.Combine(workDir, "launcher.apk");
        await DownloadAsync(ApkUrl, apk);
        Log($"Downloaded {new FileInfo(apk).Length / 1024} KB.");
        return apk;
    }

    static async Task DownloadAsync(string url, string path)
    {
        using var http = new HttpClient();
        using var resp = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
        if (!resp.IsSuccessStatusCode)
            throw new Exception($"Download failed: HTTP {(int)resp.StatusCode} for {url} (is there a published release?)");
        await using var fs = File.Create(path);
        await resp.Content.CopyToAsync(fs);
    }

    async Task<int> Adb(string adbPath, string args)
    {
        var psi = new ProcessStartInfo(adbPath, args)
        {
            RedirectStandardOutput = true, RedirectStandardError = true,
            UseShellExecute = false, CreateNoWindow = true,
            StandardOutputEncoding = Encoding.UTF8, StandardErrorEncoding = Encoding.UTF8
        };
        using var p = Process.Start(psi)!;
        var stdout = p.StandardOutput.ReadToEndAsync();
        var stderr = p.StandardError.ReadToEndAsync();
        await p.WaitForExitAsync();
        foreach (var line in ((await stdout) + (await stderr)).Split('\n', StringSplitOptions.RemoveEmptyEntries))
            Log("  " + line.TrimEnd());
        return p.ExitCode;
    }
}
