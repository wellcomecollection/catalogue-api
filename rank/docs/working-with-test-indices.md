# Working with test indices

Often we need to be able to develop against an index that has new or altered analyzers, mappings, or settings. 

To create a new index with new settings:
* edit the relevant file in [`/indices/{namespace}`]
* run yarn `createTestIndex {namespace} {testIndeName}`

