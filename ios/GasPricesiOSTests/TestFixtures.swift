import Foundation
@testable import GasPricesiOS

enum TestFixtures {
    static func sampleResponse() -> GasPricesResponse {
        GasPricesResponse(
            version: "2.0",
            categoryClass: "dataset",
            label: "Average weekly fuel prices",
            update: "2026-04-10T10:00:00",
            href: "http://datacube.statistics.sk/",
            dimension: Dimension(
                sp0207ts_tyz: Category(
                    label: "Week",
                    note: nil,
                    category: CategoryDetail(
                        index: ["w0": 0, "w1": 1],
                        label: [
                            "w0": "15. week (06.04.2026-12.04.2026)",
                            "w1": "14. week (30.03.2026-05.04.2026)"
                        ]
                    )
                ),
                sp0207ts_ukaz: Category(
                    label: "Indicator",
                    note: nil,
                    category: CategoryDetail(index: ["UKAZ01": 0, "UKAZ02": 1, "UKAZ04": 2], label: [
                        "UKAZ01": "95 Octane",
                        "UKAZ02": "98 Octane",
                        "UKAZ04": "Diesel"
                    ])
                ),
                sp0207ts_data: Category(
                    label: "Data",
                    note: nil,
                    category: CategoryDetail(index: [:], label: [:])
                )
            ),
            value: [1.500, 1.600, 1.450, 1.490, 1.590, 1.430]
        )
    }

    static func sampleJSON() -> Data {
        let json = """
        {
          "version": "2.0",
          "class": "dataset",
          "label": "Average weekly fuel prices",
          "update": "2026-04-10T10:00:00",
          "href": "http://datacube.statistics.sk/",
          "dimension": {
            "sp0207ts_tyz": {
              "label": "Week",
              "note": "",
              "category": {
                "index": {"w0": 0, "w1": 1},
                "label": {
                  "w0": "15. week (06.04.2026-12.04.2026)",
                  "w1": "14. week (30.03.2026-05.04.2026)"
                }
              }
            },
            "sp0207ts_ukaz": {
              "label": "Indicator",
              "note": "",
              "category": {
                "index": {"UKAZ01": 0, "UKAZ02": 1, "UKAZ04": 2},
                "label": {
                  "UKAZ01": "95 Octane",
                  "UKAZ02": "98 Octane",
                  "UKAZ04": "Diesel"
                }
              }
            },
            "sp0207ts_data": {
              "label": "Data",
              "note": "",
              "category": {
                "index": {},
                "label": {}
              }
            }
          },
          "value": [1.500, 1.600, 1.450, 1.490, 1.590, 1.430]
        }
        """

        return Data(json.utf8)
    }
}