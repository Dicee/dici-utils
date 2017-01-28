Todo:

- fix bug in `listAllSequences` when calling on an empty trie
- implement autocompleter with unbounded trie
- implement autocompleter with bounded trie and old/young generations to decide which words must remain in the trie
- add tests for the two above
- implement serialization/deserialization for the weighted trie
- handle uppercase/lowercase
- simple UI for autocompleters
- chill and be proud

Done:

- generalize to support custom logic and data-structures for storing the hierarchy of nodes
- dummy implementation of the above interface using a red-black tree of hash maps
- implement a default hash map based trie and a trie using weighted branches that can be traversed in-order
- add tests for the above subclasses of `Trie`

Won't do:

- interface for a sorted data-structure that can be queried using a pair of a sort key and a hash key