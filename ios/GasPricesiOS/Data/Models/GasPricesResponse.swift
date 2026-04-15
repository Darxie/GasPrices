import Foundation

struct GasPricesResponse: Decodable {
    let version: String
    let categoryClass: String
    let label: String
    let update: String
    let href: String
    let dimension: Dimension
    let value: [Double?]

    enum CodingKeys: String, CodingKey {
        case version
        case categoryClass = "class"
        case label
        case update
        case href
        case dimension
        case value
    }
}

struct Dimension: Decodable {
    let sp0207ts_tyz: Category
    let sp0207ts_ukaz: Category
    let sp0207ts_data: Category
}

struct Category: Decodable {
    let label: String
    let note: String?
    let category: CategoryDetail

    func orderedLabels() -> [String] {
        category.orderedLabels()
    }
}

struct CategoryDetail: Decodable {
    let index: [String: Int]
    let label: [String: String]

    func orderedLabels() -> [String] {
        if !index.isEmpty {
            return index
                .sorted { $0.value < $1.value }
                .compactMap { label[$0.key] }
        }

        return label
            .sorted { $0.key < $1.key }
            .map(\.value)
    }
}