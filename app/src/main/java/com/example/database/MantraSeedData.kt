package com.example.database

object MantraSeedData {

    val categories: List<MantraCategory> = listOf(
        MantraCategory("cat_shiv", "Shiv Mantra", "शिव मंत्र", "Sacred mantras dedicated to Bhagwan Shiva", 1),
        MantraCategory("cat_gayatri", "Gayatri Mantra", "गायत्री मंत्र", "Vedic illumination and wisdom mantra", 2),
        MantraCategory("cat_mahamrityunjaya", "Mahamrityunjaya Mantra", "महामृत्युंजय मंत्र", "Healing and protective Rudra mantra", 3),
        MantraCategory("cat_durga", "Durga Mantra", "दुर्गा मंत्र", "Shakti and Navarna Devi mantras", 4),
        MantraCategory("cat_hanuman", "Hanuman Mantra", "हनुमान मंत्र", "Strength, devotion, and courage mantras", 5),
        MantraCategory("cat_ganesh", "Ganesh Mantra", "गणेश मंत्र", "Vighnaharta auspicious invocation mantras", 6),
        MantraCategory("cat_vishnu", "Vishnu Mantra", "विष्णु मंत्र", "Preservation, peace, and Narayan mantras", 7),
        MantraCategory("cat_saraswati", "Saraswati Mantra", "सरस्वती मंत्र", "Vidya, music, and wisdom mantras", 8),
        MantraCategory("cat_custom", "Custom Mantra", "व्यक्तिगत मंत्र", "Your personal added mantras and shlokas", 9)
    )

    val mantras: List<Mantra> = listOf(
        Mantra(
            id = "shiv_om_namah_shivaya",
            categoryId = "cat_shiv",
            title = "Panchakshari Shiv Mantra",
            sanskritText = "ॐ नमः शिवाय",
            transliteration = "Om Namah Shivaya",
            hindiMeaning = "मैं परम कल्याणकारी भगवान शिव को नमन करता हूँ।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "gayatri_veda_mantra",
            categoryId = "cat_gayatri",
            title = "Gayatri Mantra",
            sanskritText = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्",
            transliteration = "Om Bhur Bhuvah Svah Tat Savitur Varenyam Bhargo Devasya Dhimahi Dhiyo Yo Nah Prachodayat",
            hindiMeaning = "हम उस प्राणस्वरूप, दुःखनाशक, सुखस्वरूप, श्रेष्ठ, तेजस्वी सविता देव का ध्यान करते हैं; वह हमारी बुद्धि को सन्मार्ग की ओर प्रेरित करें।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "mahamrityunjaya_mantra",
            categoryId = "cat_mahamrityunjaya",
            title = "Mahamrityunjaya Mantra",
            sanskritText = "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम् उर्वारुकमिव बन्धनान् मृत्योर्मुक्षीय मामृतात्",
            transliteration = "Om Tryambakam Yajamahe Sugandhim Pushtivardhanam Urvarukamiva Bandhanan Mrityor Mukshiya Mamritat",
            hindiMeaning = "हम त्रिनेत्र भगवान शिव की आराधना करते हैं जो जीवन में सुगंध और पुष्टि प्रदान करते हैं; जैसे पका हुआ फल बेल से सहज मुक्त होता है, वैसे ही वे हमें मृत्यु के बंधन से मुक्त कर अमृतत्व प्रदान करें।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "durga_navarna_mantra",
            categoryId = "cat_durga",
            title = "Navarna Durga Mantra",
            sanskritText = "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",
            transliteration = "Om Aim Hreem Kleem Chamundayai Vichche",
            hindiMeaning = "ज्ञान, दिव्य शक्ति और सृजन की अधिष्ठात्री माँ चामुण्डा को हमारा नमन है।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "durga_sarva_mangala",
            categoryId = "cat_durga",
            title = "Sarva Mangala Mantra",
            sanskritText = "सर्वमङ्गलमाङ्गल्ये शिवे सर्वार्थसाधिके शरण्ये त्र्यम्बके गौरि नारायणि नमोऽस्तु ते",
            transliteration = "Sarva Mangala Mangalye Shive Sarvartha Sadhike Sharanye Tryambake Gauri Narayani Namostute",
            hindiMeaning = "समस्त मंगलों की दात्री, कल्याणमयी, सब पुरुषार्थ सिद्ध करने वाली, शरणागतवत्सला त्रिनेत्री गौरी नारायणी को मेरा नमस्कार है।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "hanuman_mool_mantra",
            categoryId = "cat_hanuman",
            title = "Hanuman Mool Mantra",
            sanskritText = "ॐ हं हनुमते नमः",
            transliteration = "Om Ham Hanumate Namah",
            hindiMeaning = "अतुलित बल और भक्ति के प्रतीक श्री हनुमान जी को नमन है।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "ganesh_mool_mantra",
            categoryId = "cat_ganesh",
            title = "Ganesh Mool Mantra",
            sanskritText = "ॐ गं गणपतये नमः",
            transliteration = "Om Gam Ganapataye Namah",
            hindiMeaning = "विघ्नहर्ता श्री गणेश जी को मेरा नमन है।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "ganesh_vakratunda",
            categoryId = "cat_ganesh",
            title = "Vakratunda Mahakaya",
            sanskritText = "वक्रतुण्ड महाकाय सूर्यकोटि समप्रभ निर्विघ्नं कुरु मे देव सर्वकार्येषु सर्वदा",
            transliteration = "Vakratunda Mahakaya Suryakoti Samaprabha Nirvighnam Kuru Me Deva Sarvakaryeshu Sarvada",
            hindiMeaning = "हे घुमावदार सूंड और विशाल काया वाले, करोड़ों सूर्यों के समान तेजस्वी गणेश देव, मेरे सभी कार्यों को सदैव निर्विघ्न पूर्ण कीजिए।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "vishnu_dwadasakshari",
            categoryId = "cat_vishnu",
            title = "Vishnu Dwadasakshari Mantra",
            sanskritText = "ॐ नमो भगवते वासुदेवाय",
            transliteration = "Om Namo Bhagavate Vasudevaya",
            hindiMeaning = "सर्वव्यापी भगवान श्री वासुदेव (विष्णु) को मेरा सादर प्रणाम है।",
            defaultTargetCount = 108,
            isCustom = false
        ),
        Mantra(
            id = "saraswati_vandana_mantra",
            categoryId = "cat_saraswati",
            title = "Saraswati Vidya Mantra",
            sanskritText = "ॐ ऐं सरस्वत्यै नमः",
            transliteration = "Om Aim Saraswatyai Namah",
            hindiMeaning = "विद्या, वाणी और विवेक की देवी माँ सरस्वती को मेरा नमन है।",
            defaultTargetCount = 108,
            isCustom = false
        )
    )
}
