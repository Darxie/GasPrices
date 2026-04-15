import Foundation

struct FuelPriceTriple: Equatable {
    let gasoline95: Double?
    let gasoline98: Double?
    let diesel: Double?
}

enum GasPriceTransformer {
    static func weekLabels(from response: GasPricesResponse) -> (trimmed: [String], full: [String]) {
        let allWeeksLabels = response.dimension.sp0207ts_tyz.orderedLabels()
        let fullWeeksLabels = Array(allWeeksLabels.reversed())
        let trimmed = fullWeeksLabels.map(trimWeekLabel(_:))
        return (trimmed, fullWeeksLabels)
    }

    static func latestWeekLabel(from response: GasPricesResponse) -> String? {
        response.dimension.sp0207ts_tyz.orderedLabels().first
    }

    static func latestAvailableFuelPrices(from values: [Double?]) -> FuelPriceTriple {
        let groupCount = values.count / 3
        for groupIndex in 0..<groupCount {
            let baseIndex = groupIndex * 3
            let price95: Double? = baseIndex < values.count ? values[baseIndex] : nil
            let price98: Double? = (baseIndex + 1) < values.count ? values[baseIndex + 1] : nil
            let priceDiesel: Double? = (baseIndex + 2) < values.count ? values[baseIndex + 2] : nil
            if price95 != nil || price98 != nil || priceDiesel != nil {
                return FuelPriceTriple(gasoline95: price95, gasoline98: price98, diesel: priceDiesel)
            }
        }

        return FuelPriceTriple(gasoline95: nil, gasoline98: nil, diesel: nil)
    }

    static func createEntries(values: [Double?], typeIndex: Int, numWeeks: Int) -> [PriceEntry] {
        let entries = values.enumerated().compactMap { index, price -> PriceEntry? in
            guard index % 3 == typeIndex else {
                return nil
            }

            guard let price else {
                return nil
            }

            let groupIndex = index / 3
            let xValue = Double(numWeeks - 1) - Double(groupIndex)
            return PriceEntry(x: xValue, y: price)
        }

        return entries.sorted { $0.x < $1.x }
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

    static func buildShareText(response: GasPricesResponse, locale: Locale = .current) -> String {
        let latestPrices = latestAvailableFuelPrices(from: response.value)
        var lines = [String]()
        lines.append(response.label)

        if let latestWeekLabel = latestWeekLabel(from: response), !latestWeekLabel.isEmpty {
            lines.append(latestWeekLabel)
        }

        lines.append("\(AppStrings.octane95Label): \(formatPrice(latestPrices.gasoline95, locale: locale)) EUR/l")
        lines.append("\(AppStrings.octane98Label): \(formatPrice(latestPrices.gasoline98, locale: locale)) EUR/l")
        lines.append("\(AppStrings.dieselLabel): \(formatPrice(latestPrices.diesel, locale: locale)) EUR/l")
        lines.append(AppStrings.updatedLabel(response.update))
        lines.append(AppStrings.sourceLabel(response.href))

        return lines.joined(separator: "\n")
    }

    private static func trimWeekLabel(_ label: String) -> String {
        guard let openParen = label.firstIndex(of: "("),
              let closeParen = label.lastIndex(of: ")"),
              openParen < closeParen else {
            return label
        }

        let start = label.index(after: openParen)
        return String(label[start..<closeParen])
    }
}