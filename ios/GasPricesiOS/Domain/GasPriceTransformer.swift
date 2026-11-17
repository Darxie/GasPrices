import Foundation

struct FuelPriceTriple: Equatable {
    let gasoline95: Double?
    let gasoline98: Double?
    let diesel: Double?
}

enum GasPriceTransformer {
    /// Short axis labels ("26.10.-01.11.") and full marker labels ("W44 2026 (26.10.2026-01.11.2026)"),
    /// both in chronological order.
    static func weekLabels(from data: GasPriceData) -> (trimmed: [String], full: [String]) {
        let trimmed = data.weeks.map { "\(formatDate($0.dateFrom))-\(formatDate($0.dateTo))" }
        let full = data.weeks.map(fullWeekLabel(_:))
        return (trimmed, full)
    }

    static func latestWeekLabel(from data: GasPriceData) -> String? {
        data.weeks.last.map(fullWeekLabel(_:))
    }

    static func latestAvailableFuelPrices(from weeks: [WeeklyFuelPrices]) -> FuelPriceTriple {
        guard let latest = weeks.last(where: { $0.gasoline95 != nil || $0.gasoline98 != nil || $0.diesel != nil }) else {
            return FuelPriceTriple(gasoline95: nil, gasoline98: nil, diesel: nil)
        }
        return FuelPriceTriple(gasoline95: latest.gasoline95, gasoline98: latest.gasoline98, diesel: latest.diesel)
    }

    /// One entry per week that has a price; x is the week's index in chronological order.
    static func createEntries(weeks: [WeeklyFuelPrices], price: (WeeklyFuelPrices) -> Double?) -> [PriceEntry] {
        weeks.enumerated().compactMap { index, week in
            price(week).map { PriceEntry(x: Double(index), y: $0) }
        }
    }

    static func formatPrice(_ value: Double?, addCurrency: Bool = false, locale: Locale = .current) -> String {
        guard let value else {
            return AppStrings.notAvailableLabel
        }

        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 3
        formatter.maximumFractionDigits = 3

        let formatted = formatter.string(from: NSNumber(value: value)) ?? String(format: "%.3f", value)
        return addCurrency ? "\(formatted) \u{20AC}" : formatted
    }

    static func buildShareText(data: GasPriceData, locale: Locale = .current) -> String {
        let latestPrices = latestAvailableFuelPrices(from: data.weeks)
        var lines = [String]()
        lines.append(data.label)

        if let latestWeekLabel = latestWeekLabel(from: data), !latestWeekLabel.isEmpty {
            lines.append(latestWeekLabel)
        }

        lines.append("\(AppStrings.octane95Label): \(formatPrice(latestPrices.gasoline95, locale: locale)) EUR/l")
        lines.append("\(AppStrings.octane98Label): \(formatPrice(latestPrices.gasoline98, locale: locale)) EUR/l")
        lines.append("\(AppStrings.dieselLabel): \(formatPrice(latestPrices.diesel, locale: locale)) EUR/l")
        lines.append(AppStrings.updatedLabel(data.updated))
        lines.append(AppStrings.sourceLabel(data.sourceURL))

        return lines.joined(separator: "\n")
    }

    static func fullWeekLabel(_ week: WeeklyFuelPrices) -> String {
        "\(week.week) \(week.year) (\(formatDate(week.dateFrom, withYear: true))-\(formatDate(week.dateTo, withYear: true)))"
    }

    /// Converts ISO "2026-10-26" to "26.10." (or "26.10.2026" with year).
    static func formatDate(_ isoDate: String, withYear: Bool = false) -> String {
        let parts = isoDate.split(separator: "-")
        guard parts.count == 3 else {
            return isoDate
        }
        return withYear ? "\(parts[2]).\(parts[1]).\(parts[0])" : "\(parts[2]).\(parts[1])."
    }
}
