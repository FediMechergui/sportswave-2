using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;
using System.Reflection;
using System.Text.Json;

namespace SportsWave;

public class MainWindow : Form
{
    readonly WebView2 browser = new() { Dock = DockStyle.Fill };
    readonly string data = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SportsWave", "Desktop");
    static readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(20) };
    public MainWindow()
    {
        Text = "SportsWave • Windows preview"; Width = 1240; Height = 850; MinimumSize = new Size(800, 600);
        BackColor = Color.FromArgb(11, 15, 18); Controls.Add(browser);
        Icon = Icon.ExtractAssociatedIcon(Environment.ProcessPath!);
        Shown += async (_, _) => {
            try { await Initialize(); }
            catch (Exception ex) { MessageBox.Show("SportsWave could not start. Install Microsoft Edge WebView2 Runtime if missing.\n\n" + ex.Message, "SportsWave"); Close(); }
        };
    }
    async Task Initialize()
    {
        Directory.CreateDirectory(data);
        var ui = Path.Combine(data, "ui-0.1.0"); Directory.CreateDirectory(ui);
        foreach (var resource in Assembly.GetExecutingAssembly().GetManifestResourceNames().Where(n => n.StartsWith("SportsWave.ui."))) {
            using var input = Assembly.GetExecutingAssembly().GetManifestResourceStream(resource)!;
            using var output = File.Create(Path.Combine(ui, resource["SportsWave.ui.".Length..])); input.CopyTo(output);
        }
        await browser.EnsureCoreWebView2Async(await CoreWebView2Environment.CreateAsync(null, Path.Combine(data, "browser")));
        var core = browser.CoreWebView2;
        core.SetVirtualHostNameToFolderMapping("sportswave.local", ui, CoreWebView2HostResourceAccessKind.DenyCors);
        core.Settings.AreDevToolsEnabled = false;
        core.NewWindowRequested += (_, e) => e.Handled = true;
        core.PermissionRequested += (_, e) => e.State = CoreWebView2PermissionState.Deny;
        core.NavigationStarting += (_, e) => { if (!e.Uri.StartsWith("https://sportswave.local/", StringComparison.Ordinal)) e.Cancel = true; };
        core.WebMessageReceived += async (_, e) => {
            if (!e.Source.StartsWith("https://sportswave.local/", StringComparison.Ordinal)) return;
            try {
                using var doc = JsonDocument.Parse(e.WebMessageAsJson); var m = doc.RootElement;
                var id = m.GetProperty("id").GetInt32(); var action = m.GetProperty("action").GetString();
                object? result = action switch {
                    "schedule" => await Schedule(m.GetProperty("date").GetString()!),
                    "load" => File.Exists(Path.Combine(data, "catalog.json")) ? JsonSerializer.Deserialize<JsonElement>(await File.ReadAllTextAsync(Path.Combine(data, "catalog.json"))) : null,
                    "import" => Import(),
                    _ => throw new InvalidOperationException("Unknown command")
                };
                core.PostWebMessageAsJson(JsonSerializer.Serialize(new { id, result }));
            } catch (Exception ex) {
                using var doc = JsonDocument.Parse(e.WebMessageAsJson);
                core.PostWebMessageAsJson(JsonSerializer.Serialize(new { id = doc.RootElement.GetProperty("id").GetInt32(), error = ex.Message }));
            }
        };
        core.ContainsFullScreenElementChanged += (_, _) => {
            FormBorderStyle = core.ContainsFullScreenElement ? FormBorderStyle.None : FormBorderStyle.Sizable;
            WindowState = core.ContainsFullScreenElement ? FormWindowState.Maximized : FormWindowState.Normal;
        };
        core.Navigate("https://sportswave.local/index.html");
    }
    object? Import()
    {
        using var dialog = new OpenFileDialog { Filter = "SportsWave catalog (*.json)|*.json" };
        if (dialog.ShowDialog(this) != DialogResult.OK) return null;
        if (new FileInfo(dialog.FileName).Length > 4_000_000) throw new Exception("Catalog exceeds 4 MB.");
        var text = File.ReadAllText(dialog.FileName); using var doc = JsonDocument.Parse(text);
        var root = doc.RootElement;
        var sources = root.ValueKind == JsonValueKind.Array ? root : root.GetProperty("sources");
        if (sources.ValueKind != JsonValueKind.Array) throw new Exception("Expected a sources array.");
        foreach (var source in sources.EnumerateArray()) {
            _ = source.GetProperty("name").GetString();
            if (!source.TryGetProperty("url", out var u) || !Uri.TryCreate(u.GetString(), UriKind.Absolute, out var uri) || uri.Scheme != "https")
                throw new Exception("Windows preview imports direct HTTPS media sources only. Android resolver entries need porting.");
            if ((source.TryGetProperty("headers", out var headers) && headers.EnumerateObject().Any()) || source.TryGetProperty("licenseUrl", out _) || source.TryGetProperty("resolverUrl", out _))
                throw new Exception("Custom headers, DRM and resolver sources are not supported in this preview.");
        }
        File.WriteAllText(Path.Combine(data, "catalog.json"), text);
        return root.Clone();
    }
    static async Task<object> Schedule(string date)
    {
        var day = DateOnly.ParseExact(date, "yyyy-MM-dd");
        var feeds = new[] { ("soccer/all", "Football"), ("basketball/nba", "Basketball"), ("football/nfl", "American football"), ("hockey/nhl", "Ice hockey"), ("baseball/mlb", "Baseball"), ("tennis/atp", "Tennis"), ("racing/f1", "Motorsport") };
        var results = await Task.WhenAll(feeds.Select(async feed => {
            try {
                var json = await http.GetStringAsync($"https://site.api.espn.com/apis/site/v2/sports/{feed.Item1}/scoreboard?dates={day:yyyyMMdd}&limit=1000");
                using var doc = JsonDocument.Parse(json);
                return (sport: feed.Item2, events: doc.RootElement.GetProperty("events").Clone(), error: "");
            } catch (Exception) { return (sport: feed.Item2, events: JsonSerializer.SerializeToElement(Array.Empty<object>()), error: feed.Item2); }
        }));
        return new { feeds = results.Select(r => new { sport = r.sport, events = r.events }), errors = results.Where(r => r.error != "").Select(r => r.error), fetchedAt = DateTimeOffset.UtcNow };
    }
}
