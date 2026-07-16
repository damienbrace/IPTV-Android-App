package com.example.iptvapp.data.model

internal const val SuperscriptLiveMarker = "ᴸᶦᵛᵉ"

private val sportsContextPattern = Regex(
    """\b(sports?|espn|football|soccer|cricket|rugby|tennis|golf|baseball|softball|basketball|volleyball|badminton|hockey|cycling|athletics|netball|snooker|darts|boxing|mma|ufc|wrestling|nfl|nba|nhl|mlb|afl|nrl|pga|atp|wta|bwf|fifa|uefa|motorsports?|motogp|nascar|indycar|formula\s*1|f1)\b""",
    RegexOption.IGNORE_CASE
)
private val nonGamePattern = Regex(
    """\b(news|weather|highlights?|replays?|repeats?|reviews?|previews?|build[- ]?ups?|classics?|archives?|reruns?|documentar(?:y|ies)|magazine|talk|analysis|countdown|scoreboards?|sportscent(?:er|re)|sportsnite|sports\s*30|shows?)\b""",
    RegexOption.IGNORE_CASE
)
private val horseRacingPattern = Regex(
    """\b(horse|horses|equine|thoroughbred|harness|greyhound|racing|raceday|racecards?)\b""",
    RegexOption.IGNORE_CASE
)
private val motorRacingPattern = Regex(
    """\b(motorsports?|motor\s+racing|motogp|nascar|indycar|formula\s*1|f1|supercars?|superbikes?)\b""",
    RegexOption.IGNORE_CASE
)

internal fun isCurrentLiveSportsTitle(
    title: String,
    channelName: String,
    channelCategory: String
): Boolean {
    if (SuperscriptLiveMarker !in title) return false
    val context = "$title $channelName $channelCategory"
    if (nonGamePattern.containsMatchIn(context)) return false
    if (horseRacingPattern.containsMatchIn(context) && !motorRacingPattern.containsMatchIn(context)) return false
    return sportsContextPattern.containsMatchIn(context)
}
