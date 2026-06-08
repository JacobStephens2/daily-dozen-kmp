package page.stephens.dailydozen.data.sync

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private typealias DataMap = Map<String, Map<String, List<Int>>>

/**
 * The merge laws as CI gates (GPT-5.5). Set union is a join-semilattice, so the
 * merge must be idempotent, commutative, associative, and monotonic. A future
 * refactor that breaks any of these would silently reintroduce data loss — these
 * tests make that impossible to merge.
 */
class MergeLawsTest {

    private fun randomData(rnd: Random): DataMap =
        listOf("Thu Jan 16 2025", "Fri Jan 17 2025", "Sat Jan 18 2025")
            .filter { rnd.nextBoolean() }
            .associateWith {
                listOf("beans", "greens", "protein", "berries")
                    .filter { rnd.nextBoolean() }
                    .associateWith { (0..5).filter { rnd.nextBoolean() } }
                    .filterValues { it.isNotEmpty() }
            }
            .filterValues { it.isNotEmpty() }

    // ----- index-set primitives -----

    @Test
    fun indexSetUnionIsSortedAndDeduped() {
        assertEquals(listOf(0, 1, 2), mergeIndexSets(listOf(2, 0, 2, 1), listOf(1, 0)))
    }

    @Test
    fun indexSetUnionIsIdempotent() {
        val a = listOf(0, 2, 5)
        assertEquals(a, mergeIndexSets(a, a))
    }

    // ----- data-map laws (property-based over seeded random inputs) -----

    @Test
    fun commutative() {
        val rnd = Random(0xDD)
        repeat(200) {
            val a = randomData(rnd)
            val b = randomData(rnd)
            assertEquals(mergeDataMaps(a, b), mergeDataMaps(b, a))
        }
    }

    @Test
    fun associative() {
        val rnd = Random(0xD02)
        repeat(200) {
            val a = randomData(rnd)
            val b = randomData(rnd)
            val c = randomData(rnd)
            assertEquals(
                mergeDataMaps(mergeDataMaps(a, b), c),
                mergeDataMaps(a, mergeDataMaps(b, c)),
            )
        }
    }

    @Test
    fun idempotent() {
        val rnd = Random(0x1D)
        repeat(200) {
            val a = randomData(rnd)
            // a may be non-canonical; merging with itself canonicalizes, and a
            // second merge is a fixed point.
            val once = mergeDataMaps(a, a)
            assertEquals(once, mergeDataMaps(once, once))
        }
    }

    @Test
    fun monotonic_neitherInputLosesAnIndex() {
        val rnd = Random(0x3E)
        repeat(200) {
            val a = randomData(rnd)
            val b = randomData(rnd)
            val merged = mergeDataMaps(a, b)
            for (input in listOf(a, b)) {
                for ((day, cats) in input) {
                    for ((cat, indices) in cats) {
                        val mergedCell = merged[day]?.get(cat)?.toSet() ?: emptySet()
                        assertTrue(
                            mergedCell.containsAll(indices.toSet()),
                            "lost indices at $day/$cat: had $indices, merged has $mergedCell",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun unCheckReappears_growOnlyByDesign() {
        // Device A unchecked index 1; device B still has it. Union keeps it (§1.2).
        val a = mapOf("Thu Jan 16 2025" to mapOf("beans" to listOf(0)))
        val b = mapOf("Thu Jan 16 2025" to mapOf("beans" to listOf(0, 1)))
        assertEquals(listOf(0, 1), mergeDataMaps(a, b).getValue("Thu Jan 16 2025").getValue("beans"))
    }
}
