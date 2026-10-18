// Deterministic code-analysis reports: ./gradlew analysisReport, then open app/build/reports/index.html.
// Same code in, same reports out, so reports can be compared before and after a change.

val appPackage = "com.satory.graphenosai"
val reportsDir = layout.buildDirectory.dir("reports").get().asFile
val archDir = File(reportsDir, "architecture")
val classesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
val jdepsTmpDir = layout.buildDirectory.dir("tmp/jdeps").get().asFile
val jdeps = File(System.getProperty("java.home"), "bin/jdeps").path

val reportLinks = listOf(
    "Code complexity and smells (Detekt)" to "detekt/detekt.html",
    "Test coverage (Kover)" to "kover/htmlDebug/index.html",
    "Unit test results" to "tests/testDebugUnitTest/index.html",
    "API documentation (Dokka)" to "../dokka/html/index.html",
)

fun runCommand(vararg cmd: String) {
    val exitCode = ProcessBuilder(*cmd).inheritIO().start().waitFor()
    check(exitCode == 0) { "Command failed ($exitCode): ${cmd.joinToString(" ")}" }
}

/** Folds Kotlin-generated classes into the class they belong to, e.g. `ui.ComposableSingletons$SettingsScreenKt$lambda-1$1` -> `ui.SettingsScreen`. */
fun fold(className: String): String {
    val name = className.removeSuffix(" (debug)").removePrefix(appPackage).removePrefix(".")
        .replace("ComposableSingletons$", "")
        .substringBefore('$')
        .removeSuffix("Kt")
    return name.ifEmpty { "(root)" }
}

/** Dependencies between our own classes or packages, as sorted (from, to) pairs. */
fun jdepsEdges(verbose: String): List<Pair<String, String>> {
    jdepsTmpDir.deleteRecursively()
    runCommand(jdeps, "-verbose:$verbose", "-filter:none", "--regex", appPackage.replace(".", "\\.") + ".*",
        "-dotoutput", jdepsTmpDir.path, classesDir.path)
    val dot = File(jdepsTmpDir, "debug.dot").readText()
    val edge = Regex("\"([^\"]+)\"\\s*->\\s*\"([^\"]+)\"")
    return edge.findAll(dot)
        // "(not found)" targets are Android's generated R resource classes, compiled outside this directory.
        .filter { "(not found)" !in it.groupValues[2] }
        .map { fold(it.groupValues[1]) to fold(it.groupValues[2]) }
        .filter { (a, b) -> a != b && a != "R" && b != "R" }
        .distinct()
        .sortedWith(compareBy({ it.first }, { it.second }))
        .toList()
}

fun packageOf(node: String) = if ('.' in node) node.substringBeforeLast('.') else "(root)"

fun toDot(edges: List<Pair<String, String>>, clustered: Boolean): String {
    val lines = mutableListOf("digraph G {", "  rankdir=LR;", "  node [shape=box, style=rounded, fontname=\"Helvetica\"];")
    if (clustered) {
        val nodes = edges.flatMap { listOf(it.first, it.second) }.distinct().sorted()
        for (pkg in nodes.map(::packageOf).distinct().sorted()) {
            lines += "  subgraph \"cluster_$pkg\" { label=\"$pkg\"; style=dashed;"
            lines += nodes.filter { packageOf(it) == pkg }.map { "    \"$it\" [label=\"${it.substringAfterLast('.')}\"];" }
            lines += "  }"
        }
    }
    lines += edges.map { (a, b) -> "  \"$a\" -> \"$b\";" }
    lines += "}"
    return lines.joinToString("\n") + "\n"
}

fun render(dotText: String, name: String) {
    val dotFile = File(archDir, "$name.dot")
    dotFile.writeText(dotText)
    try {
        runCommand("dot", "-Tsvg", dotFile.path, "-o", File(archDir, "$name.svg").path)
    } catch (e: java.io.IOException) {
        throw GradleException("Graphviz 'dot' not found. Install it: brew install graphviz / apt install graphviz", e)
    }
}

fun writeIndex() {
    val links = reportLinks.joinToString("\n") { (title, href) -> "<li><a href=\"$href\">$title</a></li>" }
    val graphs = listOf("Package dependencies" to "packages", "Class dependencies" to "classes").joinToString("\n") { (title, n) ->
        "<h2>$title</h2><a href=\"architecture/$n.svg\"><img src=\"architecture/$n.svg\"></a>"
    }
    File(reportsDir, "index.html").writeText(
        "<!doctype html><meta charset='utf-8'><title>gos-ai analysis</title>" +
            "<style>body{font-family:sans-serif;margin:2em}img{max-width:100%;border:1px solid #ccc}</style>" +
            "<h1>gos-ai analysis reports</h1><ul>$links</ul>$graphs\n"
    )
}

tasks.register("analysisDependencyGraphs") {
    description = "Draws package and class dependency graphs from the compiled debug classes."
    dependsOn("compileDebugKotlin")
    doLast {
        archDir.mkdirs()
        render(toDot(jdepsEdges("package"), clustered = false), "packages")
        render(toDot(jdepsEdges("class"), clustered = true), "classes")
    }
}

tasks.register("analysisReport") {
    group = "verification"
    description = "Generates all analysis reports into app/build/reports/index.html."
    dependsOn("testDebugUnitTest", "koverHtmlReportDebug", "detekt", "dokkaHtml", "analysisDependencyGraphs")
    doLast {
        writeIndex()
        println("Open ${File(reportsDir, "index.html")}")
    }
}
