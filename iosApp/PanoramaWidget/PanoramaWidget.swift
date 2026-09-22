import SwiftUI
import WidgetKit

// Talks to the WordPress REST API directly — same host the main app's
// ApiConfig points at (see HttpClientFactory.kt) — so there's no Firebase/App
// Group setup needed for the widget to work, matching RUNNING.md.
private let latestPostURL = URL(
    string: "https://panoramapolihnitou.gr/wp-json/wp/v2/posts?per_page=1&_fields=title"
)!

private struct PostDto: Decodable {
    let title: Title

    struct Title: Decodable {
        let rendered: String
    }
}

struct ArticleEntry: TimelineEntry {
    let date: Date
    let headline: String
}

private let placeholderEntry = ArticleEntry(date: Date(), headline: "Πανόραμα Πολιχνίτου")

struct ArticleProvider: TimelineProvider {
    func placeholder(in context: Context) -> ArticleEntry { placeholderEntry }

    func getSnapshot(in context: Context, completion: @escaping (ArticleEntry) -> Void) {
        completion(placeholderEntry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ArticleEntry>) -> Void) {
        let request = URLRequest(url: latestPostURL, timeoutInterval: 15)
        URLSession.shared.dataTask(with: request) { data, _, _ in
            let entry = Self.parseEntry(from: data) ?? placeholderEntry
            // Refreshed roughly hourly (see RUNNING.md) — WidgetKit budgets
            // background refreshes itself, this is a minimum interval, not a guarantee.
            let nextRefresh = Date().addingTimeInterval(60 * 60)
            completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
        }.resume()
    }

    private static func parseEntry(from data: Data?) -> ArticleEntry? {
        guard let data,
              let posts = try? JSONDecoder().decode([PostDto].self, from: data),
              let headline = posts.first?.title.rendered else { return nil }
        return ArticleEntry(date: Date(), headline: decodeHtmlEntities(headline))
    }
}

// WordPress titles carry HTML entities (Greek punctuation especially); this is
// a small, dependency-free subset — mirrors util/Html.kt's entityMap on the app side.
private func decodeHtmlEntities(_ raw: String) -> String {
    let entities: [String: String] = [
        "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": "\"",
        "&#039;": "'", "&#39;": "'", "&apos;": "'", "&nbsp;": " ",
        "&hellip;": "…", "&ndash;": "–", "&mdash;": "—",
        "&laquo;": "«", "&raquo;": "»",
        "&#8216;": "‘", "&#8217;": "’", "&#8220;": "“", "&#8221;": "”",
        "&#8211;": "–", "&#8212;": "—"
    ]
    return entities.reduce(raw) { partial, entry in
        partial.replacingOccurrences(of: entry.key, with: entry.value)
    }
}

private let panoramaRed = Color(red: 0.70, green: 0.13, blue: 0.13) // matches brand_primary #B22222

private extension View {
    /// iOS 17 requires `.containerBackground` for a widget's background; earlier
    /// OS versions don't have that modifier and just take a plain `.background`.
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(panoramaRed, for: .widget)
        } else {
            background(panoramaRed)
        }
    }
}

struct PanoramaWidgetEntryView: View {
    var entry: ArticleEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("ΠΑΝΟΡΑΜΑ ΠΟΛΙΧΝΙΤΟΥ")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundColor(.white.opacity(0.85))
            Text(entry.headline)
                .font(.headline)
                .foregroundColor(.white)
                .lineLimit(4)
                .minimumScaleFactor(0.8)
            Spacer(minLength: 0)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .widgetBackground()
    }
}

struct PanoramaWidget: Widget {
    let kind: String = "PanoramaWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ArticleProvider()) { entry in
            PanoramaWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Πανόραμα Πολιχνίτου")
        .description("Η τελευταία είδηση από το Πανόραμα Πολιχνίτου.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
