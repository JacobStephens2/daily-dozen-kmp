package page.stephens.dailydozen.domain

/** Catholic devotional content, transcribed from the web app (ground truth). */
object Devotional {

    /** The Blessing Before Meals shown by the "Give Thanks" 🙏 action. */
    val blessingLines: List<String> = listOf(
        "Bless us, O Lord, and these Thy gifts, which we are about to receive from Thy bounty, through Christ our Lord. Amen.",
        "And may the souls of the faithful departed, through the mercy of God, rest in peace. Amen.",
    )

    /** The encouragement shown in the 100%-completion celebration. */
    const val CELEBRATION_MESSAGE: String =
        "Thank you for honoring this temple of the Holy Spirit. Your care for your body " +
            "glorifies God and respects the sacred gift of life He has entrusted to you."

    data class Verse(val text: String, val reference: String)

    /** The seven scripture verses rotated in the celebration modal. */
    val verses: List<Verse> = listOf(
        Verse("Do you not know that your body is a temple of the Holy Spirit within you, whom you have from God?", "1 Corinthians 6:19"),
        Verse("So, whether you eat or drink, or whatever you do, do all to the glory of God.", "1 Corinthians 10:31"),
        Verse("Beloved, I pray that all may go well with you and that you may be in good health, as it goes well with your soul.", "3 John 1:2"),
        Verse("For everything created by God is good, and nothing is to be rejected if it is received with thanksgiving.", "1 Timothy 4:4"),
        Verse("The Lord sustains him on his sickbed; in his illness you restore him to full health.", "Psalm 41:3"),
        Verse("A joyful heart is good medicine, but a crushed spirit dries up the bones.", "Proverbs 17:22"),
        Verse("He gives food to every creature. His love endures forever.", "Psalm 136:25"),
    )

    /** Saint Martha footer note. */
    const val SAINT_MARTHA: String =
        "Saint Martha is considered the patron saint of cooks (and sometimes of dietitians) " +
            "due to her role in the Gospels, where she is portrayed as a gracious hostess and " +
            "provider of meals."
}
