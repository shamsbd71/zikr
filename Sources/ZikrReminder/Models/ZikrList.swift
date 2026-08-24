import Foundation

/// General, anytime adhkar — short phrases of remembrance drawn from the
/// well-known Hisnul Muslim (Fortress of the Muslim) collection. Kept to
/// non time-locked dhikr so a random reminder is always appropriate.
enum ZikrList {
    static let all: [Zikr] = [
        Zikr(id: 1, arabic: "سُبْحَانَ اللَّهِ", transliteration: "SubhanAllah", translation: "Glory be to Allah"),
        Zikr(id: 2, arabic: "الْحَمْدُ لِلَّهِ", transliteration: "Alhamdulillah", translation: "All praise is due to Allah"),
        Zikr(id: 3, arabic: "اللَّهُ أَكْبَرُ", transliteration: "Allahu Akbar", translation: "Allah is the Greatest"),
        Zikr(id: 4, arabic: "لَا إِلَٰهَ إِلَّا اللَّهُ", transliteration: "La ilaha illallah", translation: "There is no god but Allah"),
        Zikr(id: 5, arabic: "أَسْتَغْفِرُ اللَّهَ", transliteration: "Astaghfirullah", translation: "I seek forgiveness from Allah"),
        Zikr(id: 6, arabic: "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", transliteration: "La hawla wala quwwata illa billah", translation: "There is no power nor strength except with Allah"),
        Zikr(id: 7, arabic: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", transliteration: "SubhanAllahi wa bihamdihi", translation: "Glory be to Allah and praise be to Him"),
        Zikr(id: 8, arabic: "سُبْحَانَ اللَّهِ الْعَظِيمِ", transliteration: "SubhanAllahil Azeem", translation: "Glory be to Allah, the Magnificent"),
        Zikr(id: 9, arabic: "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", transliteration: "La ilaha illa anta subhanaka inni kuntu minaz-zalimin", translation: "There is no god but You, glory be to You, I was among the wrongdoers"),
        Zikr(id: 10, arabic: "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", transliteration: "Hasbunallahu wa ni'mal wakeel", translation: "Allah is sufficient for us, and He is the best disposer of affairs"),
        Zikr(id: 11, arabic: "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", transliteration: "Allahumma salli 'ala Muhammad", translation: "O Allah, send blessings upon Muhammad"),
        Zikr(id: 12, arabic: "رَبِّ اغْفِرْ لِي", transliteration: "Rabbighfir li", translation: "My Lord, forgive me"),
        Zikr(id: 13, arabic: "يَا حَيُّ يَا قَيُّومُ", transliteration: "Ya Hayyu Ya Qayyum", translation: "O Ever-Living, O Sustainer"),
        Zikr(id: 14, arabic: "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً", transliteration: "Rabbana atina fid-dunya hasanah", translation: "Our Lord, give us good in this world"),
        Zikr(id: 15, arabic: "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", transliteration: "Allahumma innaka 'afuwwun tuhibbul 'afwa fa'fu 'anni", translation: "O Allah, You are Forgiving and love forgiveness, so forgive me"),
        Zikr(id: 16, arabic: "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", transliteration: "La ilaha illallah wahdahu la sharika lah", translation: "There is no god but Allah alone, with no partner"),
        Zikr(id: 17, arabic: "سُبْحَانَ رَبِّيَ الْعَظِيمِ", transliteration: "Subhana Rabbiyal Azeem", translation: "Glory be to my Lord, the Magnificent"),
        Zikr(id: 18, arabic: "سُبْحَانَ رَبِّيَ الْأَعْلَى", transliteration: "Subhana Rabbiyal A'la", translation: "Glory be to my Lord, the Most High"),
        Zikr(id: 19, arabic: "رَبِّ زِدْنِي عِلْمًا", transliteration: "Rabbi zidni ilma", translation: "My Lord, increase me in knowledge"),
        Zikr(id: 20, arabic: "تَوَكَّلْتُ عَلَى اللَّهِ", transliteration: "Tawakkaltu 'alallah", translation: "I place my trust in Allah"),
        Zikr(id: 22, arabic: "بِسْمِ اللَّهِ", transliteration: "Bismillah", translation: "In the name of Allah")
    ]

    static func random() -> Zikr {
        all.randomElement()!
    }
}
