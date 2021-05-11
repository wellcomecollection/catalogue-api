# snapshot-analysis

This folder contains some scripts for analysing the catalogue snapshots.

Examples of how I use it:

-   What are all the values of a particular field?
    e.g. what are all the shelfmarks

-   What's an example of this condition?
    e.g. a work with a `miro-library-reference` identifier type

-   Are there any works with this condition?
    e.g. proving that no physical location has multiple access conditions

You can do some of these in Elasticsearch; I prefer using snapshots because I can write queries that might be fiddly or impossible to write in Elasticsearch, and having every instance of a field available is super handy.

## Usage

1.  Download a current snapshot by running:

    ```
    $ python3 download_latest_snapshot.py
    ```

    This may take a few minutes, depending on your Internet connection.

2.  Create a new Python script/Jupyter notebook with the following template:

    ```python
    from pprint import pprint
    from utils import get_works

    for work in get_works():
        pprint(work)
        break
    ```

    This will give you instances of the Work model from the catalogue API -- iterate to your heart's content.

## Examples

*   What are all the `terms` on the access conditions for physical locations?

    ```python
    import collections

    from utils import get_works

    terms = collections.Counter()

    for work in tqdm.tqdm():
      for it in work["items"]:
          for loc in it["locations"]:
              if loc["type"] == "PhysicalLocation":
                  for ac in loc["accessConditions"]:
                      terms[ac.get("terms")] += 1
    ```

*   How many instances do we have of each digcode?

    ```
    import collections

    from utils import get_works

    digcodes = collections.Counter()

    for work in get_works():
        for id in work["identifiers"]:
            if id["identifierType"]["id"] == "wellcome-digcode":
                digcodes[id["value"]] += 1
    ```

*   Are there any items with multiple physical locations?

    ```python
    from utils import get_works

    for work in get_works():
        for item in work["items"]:
            physical_locations = [
                loc
                for loc in it["locations"]
                if loc["type"] == "PhysicalLocation"
            ]

            if len(physical_locations) > 1:
                print(work["id"])
    ```
