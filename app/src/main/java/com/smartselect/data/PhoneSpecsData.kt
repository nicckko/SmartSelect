package com.smartselect.data

object PhoneSpecsData {

    val brands = listOf("Apple","Samsung","Infinix","Tecno","vivo","Honor","realme","POCO","Redmi")

    val modelsByBrand: Map<String, List<String>> = mapOf(
        "Apple" to listOf(
            "iPhone SE (2nd gen)","iPhone SE (3rd gen)",
            "iPhone 11","iPhone 11 Pro","iPhone 11 Pro Max",
            "iPhone 12 mini","iPhone 12","iPhone 12 Pro","iPhone 12 Pro Max",
            "iPhone 13 mini","iPhone 13","iPhone 13 Pro","iPhone 13 Pro Max",
            "iPhone 14","iPhone 14 Plus","iPhone 14 Pro","iPhone 14 Pro Max",
            "iPhone 15","iPhone 15 Plus","iPhone 15 Pro","iPhone 15 Pro Max",
            "iPhone 16","iPhone 16 Plus","iPhone 16 Pro","iPhone 16 Pro Max"
        ),
        "Samsung" to listOf(
            "Galaxy S21","Galaxy S21+","Galaxy S21 Ultra","Galaxy S21 FE",
            "Galaxy S22","Galaxy S22+","Galaxy S22 Ultra",
            "Galaxy S23","Galaxy S23+","Galaxy S23 Ultra","Galaxy S23 FE",
            "Galaxy S24","Galaxy S24+","Galaxy S24 Ultra","Galaxy S25","Galaxy S25+","Galaxy S25 Ultra",
            "Galaxy A05","Galaxy A05s","Galaxy A14","Galaxy A15","Galaxy A24","Galaxy A25",
            "Galaxy A34","Galaxy A35","Galaxy A53","Galaxy A54","Galaxy A55",
            "Galaxy M14","Galaxy M34","Galaxy M54","Galaxy M55",
            "Galaxy Z Fold 5","Galaxy Z Fold 6","Galaxy Z Flip 5","Galaxy Z Flip 6"
        ),
        "Infinix" to listOf(
            "Hot 20","Hot 30","Hot 30i","Hot 40","Hot 40 Pro",
            "Note 12","Note 30","Note 30 Pro","Note 40","Note 40 Pro",
            "Zero 5G","Zero 20","Zero 30","Zero 30 5G","Zero Ultra",
            "GT 10 Pro","GT 20 Pro",
            "Smart 7","Smart 8"
        ),
        "Tecno" to listOf(
            "Spark 8","Spark 9","Spark 10","Spark 10 Pro","Spark 20","Spark 20 Pro","Spark Go 2024",
            "Camon 18","Camon 19","Camon 19 Pro","Camon 20","Camon 20 Pro","Camon 30","Camon 30 Pro",
            "Pova 3","Pova 4","Pova 5","Pova 5 Pro","Pova 6","Pova 6 Pro",
            "Phantom V Fold","Phantom V Flip","Phantom X","Phantom X2","Phantom X2 Pro"
        ),
        "vivo" to listOf(
            "V25","V25 Pro","V27","V27 Pro","V29","V29 Pro","V30","V30 Pro","V40","V40 Pro",
            "Y17s","Y22","Y27","Y36","Y55","Y100","Y200",
            "X80","X80 Pro","X90","X90 Pro","X100","X100 Pro",
            "T1","T2","T3","iQOO Z7","iQOO Z9","iQOO Neo 9"
        ),
        "Honor" to listOf(
            "X6","X7","X8","X8a","X9","X9a","X9b",
            "70","70 Pro","80","80 Pro","90","90 Pro","200","200 Pro",
            "Magic 5","Magic 5 Pro","Magic 6","Magic 6 Pro",
            "Play 40C","Play 50"
        ),
        "realme" to listOf(
            "C30","C33","C35","C51","C53","C55","C67",
            "realme 9","realme 10","realme 11","realme 11 Pro","realme 12","realme 12 Pro","realme 13","realme 13 Pro",
            "GT Neo 3","GT 2 Pro","GT 3","GT 5","GT 5 Pro","GT 6",
            "Narzo 50","Narzo 60","Narzo 70"
        ),
        "POCO" to listOf(
            "POCO F4","POCO F4 GT","POCO F5","POCO F5 Pro","POCO F6","POCO F6 Pro",
            "POCO X4 Pro","POCO X5","POCO X5 Pro","POCO X6","POCO X6 Pro",
            "POCO M4 Pro","POCO M5","POCO M5s","POCO M6 Pro",
            "POCO C40","POCO C50","POCO C55","POCO C61","POCO C65"
        ),
        "Redmi" to listOf(
            "Redmi 10","Redmi 10A","Redmi 10C","Redmi 12","Redmi 12C","Redmi 13","Redmi 13C",
            "Redmi Note 10","Redmi Note 10 Pro","Redmi Note 10S",
            "Redmi Note 11","Redmi Note 11 Pro","Redmi Note 11 Pro 5G","Redmi Note 11S",
            "Redmi Note 12","Redmi Note 12 Pro","Redmi Note 12 Pro+",
            "Redmi Note 13","Redmi Note 13 Pro","Redmi Note 13 Pro+"
        )
    )

    // ============ BRAND-SPECIFIC FILTERED OPTIONS ============

    // Apple-only options
    val appleChipsets = listOf(
        "Apple A13 Bionic", "Apple A14 Bionic", "Apple A15 Bionic", "Apple A16 Bionic",
        "Apple A17 Pro", "Apple A18", "Apple A18 Pro"
    )

    val appleGpu = listOf("Apple GPU (5-core)", "Apple GPU (4-core)")

    val appleOs = listOf("iOS 18", "iOS 17", "iOS 16")

    // Android options (all non-Apple brands)
    val androidChipsets = listOf(
        "Snapdragon 460","Snapdragon 480","Snapdragon 480+","Snapdragon 662","Snapdragon 665","Snapdragon 680","Snapdragon 695",
        "Snapdragon 720G","Snapdragon 730G","Snapdragon 732G","Snapdragon 750G","Snapdragon 765G",
        "Snapdragon 778G","Snapdragon 778G+","Snapdragon 782G","Snapdragon 7s Gen 2","Snapdragon 7 Gen 1","Snapdragon 7 Gen 3",
        "Snapdragon 855","Snapdragon 860","Snapdragon 865","Snapdragon 870","Snapdragon 888","Snapdragon 888+",
        "Snapdragon 8 Gen 1","Snapdragon 8+ Gen 1","Snapdragon 8 Gen 2","Snapdragon 8 Gen 3","Snapdragon 8 Elite",
        "Dimensity 700","Dimensity 810","Dimensity 900","Dimensity 920","Dimensity 1080","Dimensity 1200",
        "Dimensity 6020","Dimensity 6100+","Dimensity 7020","Dimensity 7050","Dimensity 7200","Dimensity 7300",
        "Dimensity 8050","Dimensity 8100","Dimensity 8200","Dimensity 8300",
        "Dimensity 9000","Dimensity 9200","Dimensity 9200+","Dimensity 9300","Dimensity 9400",
        "Exynos 850","Exynos 1280","Exynos 1380","Exynos 2200","Exynos 2400",
        "Helio G35","Helio G70","Helio G80","Helio G85","Helio G88","Helio G95","Helio G96","Helio G99",
        "Google Tensor G3","Google Tensor G4",
        "Unisoc T606","Unisoc T612","Unisoc T616","Unisoc T618"
    )

    val androidGpu = listOf(
        "Adreno 750", "Adreno 740", "Adreno 730", "Adreno 660", "Adreno 650",
        "Mali-G710", "Mali-G78", "Mali-G77", "Immortalis-G715"
    )

    val androidOs = listOf("Android 14", "Android 13", "Android 12", "Android 11", "HarmonyOS 4", "HarmonyOS 3")

    // Helper functions to get filtered options based on brand
    fun getFilteredChipsets(brand: String): List<String> {
        return if (brand == "Apple") appleChipsets else androidChipsets
    }

    fun getFilteredGpu(brand: String): List<String> {
        return if (brand == "Apple") appleGpu else androidGpu
    }

    fun getFilteredOs(brand: String): List<String> {
        return if (brand == "Apple") appleOs else androidOs
    }

    // ============ EXISTING LISTS (keep as is for backup/compatibility) ============

    val chipsets = listOf(
        "Apple A13 Bionic","Apple A14 Bionic","Apple A15 Bionic","Apple A16 Bionic","Apple A17 Pro","Apple A18","Apple A18 Pro",
        "Snapdragon 460","Snapdragon 480","Snapdragon 480+","Snapdragon 662","Snapdragon 665","Snapdragon 680","Snapdragon 695",
        "Snapdragon 720G","Snapdragon 730G","Snapdragon 732G","Snapdragon 750G","Snapdragon 765G",
        "Snapdragon 778G","Snapdragon 778G+","Snapdragon 782G","Snapdragon 7s Gen 2","Snapdragon 7 Gen 1","Snapdragon 7 Gen 3",
        "Snapdragon 855","Snapdragon 860","Snapdragon 865","Snapdragon 870","Snapdragon 888","Snapdragon 888+",
        "Snapdragon 8 Gen 1","Snapdragon 8+ Gen 1","Snapdragon 8 Gen 2","Snapdragon 8 Gen 3","Snapdragon 8 Elite",
        "Dimensity 700","Dimensity 810","Dimensity 900","Dimensity 920","Dimensity 1080","Dimensity 1200",
        "Dimensity 6020","Dimensity 6100+","Dimensity 7020","Dimensity 7050","Dimensity 7200","Dimensity 7300",
        "Dimensity 8050","Dimensity 8100","Dimensity 8200","Dimensity 8300",
        "Dimensity 9000","Dimensity 9200","Dimensity 9200+","Dimensity 9300","Dimensity 9400",
        "Exynos 850","Exynos 1280","Exynos 1380","Exynos 2200","Exynos 2400",
        "Helio G35","Helio G70","Helio G80","Helio G85","Helio G88","Helio G95","Helio G96","Helio G99",
        "Google Tensor G3","Google Tensor G4",
        "Unisoc T606","Unisoc T612","Unisoc T616","Unisoc T618"
    )

    val ram = listOf("2GB","3GB","4GB","6GB","8GB","12GB","16GB","18GB","24GB")
    val storage = listOf("32GB","64GB","128GB","256GB","512GB","1TB")

    val battery = listOf(
        "3000mAh","3110mAh","3227mAh","3279mAh","3500mAh","3687mAh",
        "4000mAh","4500mAh","5000mAh","5160mAh","5500mAh","6000mAh","6080mAh","7000mAh"
    )

    val camera = listOf(
        "8MP","12MP","13MP","16MP","48MP","50MP","64MP","108MP","200MP",
        "12MP+12MP","50MP+12MP","50MP+12MP+12MP","50MP+50MP+50MP",
        "64MP+8MP+2MP+2MP","108MP+8MP+2MP","200MP+12MP+10MP","200MP+50MP+12MP",
        "48MP Triple + LiDAR","50MP+48MP+12MP"
    )

    val display = listOf(
        "5.4\" Super Retina XDR OLED 60Hz",
        "6.1\" Super Retina XDR OLED 60Hz","6.1\" AMOLED 120Hz","6.1\" Dynamic AMOLED 2X 120Hz","6.1\" IPS LCD 60Hz",
        "6.4\" AMOLED 90Hz","6.4\" IPS LCD 60Hz",
        "6.5\" IPS LCD 60Hz","6.5\" IPS LCD 90Hz","6.5\" AMOLED 90Hz","6.5\" AMOLED 120Hz",
        "6.6\" IPS LCD 60Hz","6.6\" IPS LCD 90Hz","6.6\" AMOLED 120Hz",
        "6.67\" AMOLED 120Hz","6.67\" LTPO AMOLED 1-120Hz","6.67\" Super AMOLED 120Hz",
        "6.7\" AMOLED 120Hz","6.7\" Dynamic AMOLED 2X 120Hz","6.7\" LTPO OLED 1-120Hz",
        "6.7\" ProMotion XDR OLED 1-120Hz",
        "6.8\" Dynamic AMOLED 2X 120Hz","6.8\" LTPO AMOLED 1-120Hz",
        "6.9\" LTPO AMOLED 1-120Hz","7.6\" Dynamic AMOLED 2X 120Hz"
    )

    val categories = listOf("iPhone","Android","Flagship","Mid-range","Budget","Gaming")

    val osOptions = listOf(
        "Android 14", "Android 13", "Android 12", "Android 11",
        "iOS 18", "iOS 17", "iOS 16",
        "HarmonyOS 4", "HarmonyOS 3"
    )

    val networkOptions = listOf(
        "5G", "5G Ready", "4G LTE", "4G", "3G"
    )

    val weightOptions = listOf(
        "150g", "160g", "170g", "180g", "187g", "190g", "200g", "210g", "220g", "240g"
    )

    val dimensionsOptions = listOf(
        "140.0 x 70.0 x 8.0 mm",
        "150.0 x 75.0 x 8.5 mm",
        "160.0 x 75.0 x 8.0 mm",
        "160.9 x 75.9 x 8.2 mm",
        "163.0 x 78.0 x 9.0 mm"
    )

    val buildOptions = listOf(
        "Glass front, glass back, aluminum frame",
        "Glass front, plastic back, plastic frame",
        "Glass front, glass back, stainless steel frame",
        "Plastic front, plastic back, plastic frame",
        "Glass front, silicone polymer back, aluminum frame"
    )

    val protectionOptions = listOf(
        "Corning Gorilla Glass Victus 2",
        "Corning Gorilla Glass Victus",
        "Corning Gorilla Glass 5",
        "Corning Gorilla Glass 3",
        "IP68 dust/water resistant",
        "IP67 dust/water resistant",
        "Shatterproof glass",
        "None"
    )

    val gpuOptions = listOf(
        "Adreno 750", "Adreno 740", "Adreno 730", "Adreno 660", "Adreno 650",
        "Apple GPU (5-core)", "Apple GPU (4-core)",
        "Mali-G710", "Mali-G78", "Mali-G77",
        "Immortalis-G715"
    )

    val chargingOptions = listOf(
        "15W wired", "25W wired", "33W wired", "45W wired", "65W wired", "100W wired",
        "15W wireless", "30W wireless",
        "Reverse wireless charging",
        "No fast charging"
    )

    val sensorsOptions = listOf(
        "Fingerprint (under display), accelerometer, gyro, proximity, compass, barometer",
        "Fingerprint (side-mounted), accelerometer, gyro, proximity, compass",
        "Fingerprint (rear-mounted), accelerometer, proximity",
        "Face ID, accelerometer, gyro, proximity, compass",
        "Accelerometer, gyro, proximity, compass"
    )

    val colorsOptions = listOf(
        "Black", "White", "Blue", "Green", "Red", "Purple", "Gold",
        "Titanium Black", "Titanium White", "Titanium Blue",
        "Midnight", "Starlight", "Product Red"
    )
}