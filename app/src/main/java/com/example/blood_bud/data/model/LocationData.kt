package com.example.blood_bud.data.model

data class LocationData(
    val states: List<State> = listOf()
) {
    data class State(
        val name: String,
        val cities: List<String>
    )

    companion object {
        val indianLocations = LocationData(
            states = listOf(
                State("Andhra Pradesh", listOf("Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Tirupati")),  // :contentReference[oaicite:0]{index=0}
                State("Arunachal Pradesh", listOf("Itanagar", "Tawang", "Pasighat", "Bomdila", "Ziro")),  // :contentReference[oaicite:1]{index=1}
                State("Assam", listOf("Guwahati", "Dibrugarh", "Silchar", "Tezpur", "Jorhat")),  // :contentReference[oaicite:2]{index=2}
                State("Bihar", listOf("Patna", "Gaya", "Bihar Sharif", "Darbhanga", "Bhagalpur")),  // :contentReference[oaicite:3]{index=3}
                State("Chhattisgarh", listOf("Raipur", "Bhilai", "Bilaspur", "Korba", "Rajnandgaon")),  // :contentReference[oaicite:4]{index=4}
                State("Goa", listOf("Panaji", "Vasco-da-Gama", "Margao", "Mapusa", "Ponda")),  // :contentReference[oaicite:5]{index=5}
                State("Gujarat", listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Jamnagar")),  // :contentReference[oaicite:6]{index=6}
                State("Haryana", listOf("Faridabad", "Gurgaon", "Panipat", "Ambala", "Sonipat")),  // :contentReference[oaicite:7]{index=7}
                State("Himachal Pradesh", listOf("Shimla", "Dharamshala", "Mandi", "Solan", "Chamba")),  // :contentReference[oaicite:8]{index=8}
                State("Jharkhand", listOf("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar")),  // :contentReference[oaicite:9]{index=9}
                State("Karnataka", listOf("Bengaluru", "Mysore", "Mangalore", "Hubli-Dharwad", "Davangere")),  // :contentReference[oaicite:10]{index=10}
                State("Kerala", listOf("Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Malappuram")),  // :contentReference[oaicite:11]{index=11}
                State("Madhya Pradesh", listOf("Bhopal", "Indore", "Gwalior", "Jabalpur", "Ujjain")),  // :contentReference[oaicite:12]{index=12}
                State("Maharashtra", listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad")),  // :contentReference[oaicite:13]{index=13}
                State("Manipur", listOf("Imphal", "Bishnupur", "Ukhrul", "Chandel", "Tamenglong")),  // :contentReference[oaicite:14]{index=14}
                State("Meghalaya", listOf("Shillong", "Cherrapunji", "Tura", "Jowai", "Nongpoh")),  // :contentReference[oaicite:15]{index=15}
                State("Mizoram", listOf("Aizawl", "Lunglei", "Serchhip", "Champhai", "Mamit")),  // :contentReference[oaicite:16]{index=16}
                State("Nagaland", listOf("Kohima", "Mokokchung", "Tuensang", "Zunheboto", "Phek")),  // :contentReference[oaicite:17]{index=17}
                State("Odisha", listOf("Bhubaneswar", "Cuttack", "Rourkela", "Brahmapur", "Sambalpur")),  // :contentReference[oaicite:18]{index=18}
                State("Punjab", listOf("Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Kapurthala")),  // :contentReference[oaicite:19]{index=19}
                State("Rajasthan", listOf("Jaipur", "Jodhpur", "Udaipur", "Ajmer", "Bikaner")),  // :contentReference[oaicite:20]{index=20}
                State("Sikkim", listOf("Gangtok", "Namchi", "Mangan", "Geyzing", "Rabdentse")),  // :contentReference[oaicite:21]{index=21}
                State("Tamil Nadu", listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Vellore")),  // :contentReference[oaicite:22]{index=22}
                State("Telangana", listOf("Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam")),  // :contentReference[oaicite:23]{index=23}
                State("Tripura", listOf("Agartala", "Amarpur", "Kumarghat", "Udaipur", "Gakulnagar")),  // :contentReference[oaicite:24]{index=24}
                State("Uttar Pradesh", listOf("Lucknow", "Noida", "Varanasi", "Agra", "Kanpur")),  // :contentReference[oaicite:25]{index=25}
                State("Uttarakhand", listOf("Dehradun", "Haridwar", "Roorkee", "Rishikesh", "Haldwani")),  // :contentReference[oaicite:26]{index=26}
                State("West Bengal", listOf("Kolkata", "Howrah", "Durgapur", "Asansol", "Siliguri")),  // :contentReference[oaicite:27]{index=27}
                // Union Territories also included as States
                State("Andaman and Nicobar Islands", listOf("Port Blair")),  // :contentReference[oaicite:28]{index=28}
                State("Chandigarh", listOf("Chandigarh")),  // :contentReference[oaicite:29]{index=29}
                State("Dadra and Nagar Haveli and Daman and Diu", listOf("Daman", "Diu", "Silvassa")),  // :contentReference[oaicite:30]{index=30}
                State("Delhi", listOf("New Delhi", "Delhi")),  // :contentReference[oaicite:31]{index=31}
                State("Jammu and Kashmir", listOf("Srinagar", "Jammu", "Anantnag", "Baramulla", "Kathua")),  // :contentReference[oaicite:32]{index=32}
                State("Ladakh", listOf("Leh", "Kargil")),  // :contentReference[oaicite:33]{index=33}
                State("Lakshadweep", listOf("Kavaratti")),  // :contentReference[oaicite:34]{index=34}
                State("Puducherry", listOf("Puducherry", "Karaikal", "Mahe", "Yanam")),  // :contentReference[oaicite:35]{index=35}
            )
        )
    }
}
