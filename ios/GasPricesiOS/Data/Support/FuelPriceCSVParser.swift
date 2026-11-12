import Foundation

enum FuelPriceCSVParserError: LocalizedError, Equatable {
    case missingDataFile
    case missingColumn(String)
    case invalidEncoding

    var errorDescription: String? {
        switch self {
        case .missingDataFile:
            return "No data CSV in downloaded archive."
        case .missingColumn(let name):
            return "Missing column '\(name)' in downloaded data."
        case .invalidEncoding:
            return "Downloaded data is not valid UTF-8."
        }
    }
}

/// Parses the open data CSV export of dataset sp0207ts (sp0207ts_CSV_EN.csv inside the zip).
enum FuelPriceCSVParser {
    static let codeGasoline95 = "CP_07202102"
    static let codeGasoline98 = "CP_07202103"
    static let codeDiesel = "CP_07202105"
    static let trackedCodes: Set<String> = [codeGasoline95, codeGasoline98, codeDiesel]

    struct Row: Equatable {
        let fuelCode: String
        let year: String
        let week: String
        let dateFrom: String
        let dateTo: String
        let value: String
    }

    /// Finds the data CSV at the archive root, skipping structure.csv and codelists/.
    static func parseZippedRows(_ archive: Data) throws -> [Row] {
        let zip = try ZipArchiveReader(data: archive)
        guard let entry = zip.entries.first(where: { entry in
            let name = entry.name
            return name.lowercased().hasSuffix(".csv")
                && !name.contains("/")
                && name.lowercased() != "structure.csv"
        }) else {
            throw FuelPriceCSVParserError.missingDataFile
        }
        return try parseCSV(zip.extract(entry))
    }

    /// The export holds the full history of every fuel type, so only tracked fuels are kept.
    /// Values never contain commas or quotes, so a plain split is enough.
    static func parseCSV(_ data: Data) throws -> [Row] {
        guard let text = String(data: data, encoding: .utf8) else {
            throw FuelPriceCSVParserError.invalidEncoding
        }
        var lines = text.split(whereSeparator: \.isNewline).makeIterator()
        guard let headerLine = lines.next() else {
            return []
        }
        var headerText = String(headerLine)
        if headerText.hasPrefix("\u{FEFF}") {
            headerText.removeFirst()
        }
        let header = headerText.split(separator: ",", omittingEmptySubsequences: false).map(String.init)

        func column(_ name: String) throws -> Int {
            guard let index = header.firstIndex(of: name) else {
                throw FuelPriceCSVParserError.missingColumn(name)
            }
            return index
        }
        let code = try column("e_coicop_code")
        let year = try column("e_year_code")
        let week = try column("e_week_code")
        let dateFrom = try column("e_datefrom")
        let dateTo = try column("e_dateto")
        let value = try column("value")

        var rows: [Row] = []
        while let line = lines.next() {
            let cells = line.split(separator: ",", omittingEmptySubsequences: false)
            guard cells.count == header.count else {
                continue
            }
            let fuelCode = String(cells[code])
            guard trackedCodes.contains(fuelCode) else {
                continue
            }
            rows.append(Row(
                fuelCode: fuelCode,
                year: String(cells[year]),
                week: String(cells[week]),
                dateFrom: String(cells[dateFrom]),
                dateTo: String(cells[dateTo]),
                value: String(cells[value])
            ))
        }
        return rows
    }

    /// Groups rows by week and returns the weeks in chronological order.
    static func weeklyPrices(from rows: [Row]) -> [WeeklyFuelPrices] {
        let grouped = Dictionary(grouping: rows.filter { trackedCodes.contains($0.fuelCode) && !$0.dateFrom.isEmpty },
                                 by: \.dateFrom)
        // ISO yyyy-MM-dd dates sort chronologically as strings.
        return grouped.keys.sorted().compactMap { dateFrom in
            guard let weekRows = grouped[dateFrom], let first = weekRows.first else {
                return nil
            }
            func price(_ code: String) -> Double? {
                weekRows.first { $0.fuelCode == code }.flatMap { Double($0.value) }
            }
            return WeeklyFuelPrices(
                year: first.year,
                week: first.week,
                dateFrom: dateFrom,
                dateTo: first.dateTo,
                gasoline95: price(codeGasoline95),
                gasoline98: price(codeGasoline98),
                diesel: price(codeDiesel)
            )
        }
    }
}
