# Viva preparation – one-minute answers

**Q1** Why is `+` in a loop slow? String is immutable, each `+` copies everything built so far -> O(n²). StringBuilder appends into a growing array -> O(n).
StringBuffer = same but synchronized (thread-safe per call). Per-ticket text: StringBuilder; shared log: StringBuffer (or lock whole entries).

**Q2** ArrayList: O(1) get/append, O(n) insert at front/middle (arraycopy is fast). LinkedList: O(1) at ends, O(n) get(k) and middle. Up Next queue -> ArrayDeque.

**Q3** Contract: equal objects must have equal hashCodes. HashSet finds the bucket by hashCode, calls equals only inside that bucket. No hashCode -> identity hash -> duplicates (3 instead of 2). Changing a hashed field after insertion loses the element. TreeSet uses compare, not equals.

**Q4** HashMap O(1), no order; TreeMap O(log n), sorted; LinkedHashMap O(1), insertion order. Hashtable: legacy, fully synchronized. Sorted array + binarySearch: O(log n) lookup, O(n) insert.

**Q5** Comparator chain = lexicographic order -> transitive and antisymmetric. Never compare ints by subtraction (overflow). `reversed()` goes on each key, not at the end of the chain. `? super T` for comparators (consumer), `? extends T` for lists you read from (producer).

**Q6** PriorityQueue is a heap: toString shows the array, not the sorted order. Heap is not stable -> arrival counter as tie-break. add/poll O(log n). Starvation -> aging rule.

**Q7** Original rebuilt the PriorityQueue before every serve: n(n+1)/2 patients examined. Redesign: 5 FIFO queues, compare only the 5 heads, because inside a level older patients always have equal or lower effective severity. Same order in 300 random tests.
