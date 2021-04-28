#!/usr/bin/env python3
"""
Download the latest snapshot from data.wellcomecollection.org.
"""

import datetime
import os
import uuid

import httpx
import tqdm


def download_from_url(*, url, filename):
    """
    Download a file to the given filename, with a progress bar.
    """
    if os.path.exists(filename):
        return

    # How big is the file?
    # Note: this will throw if the server doesn't return a Content-Length
    # header.  We're downloading snapshots from S3, which always does,
    # but this code may not be suitable elsewhere.
    size = int(httpx.head(url).headers["Content-Length"])

    tmp_path = filename + "." + str(uuid.uuid4()) + ".tmp"

    with open(tmp_path, "wb") as outfile:
        with tqdm.tqdm(
            total=size, unit="B", desc=os.path.basename(filename), unit_scale=True
        ) as pbar:
            with httpx.stream("GET", url) as resp:
                for chunk in resp.iter_bytes():
                    if chunk:
                        outfile.write(chunk)
                        pbar.update(len(chunk))

    os.rename(tmp_path, filename)


if __name__ == '__main__':
    download_from_url(
        url="https://data.wellcomecollection.org/catalogue/v2/works.json.gz",
        filename=f"works-{datetime.date.today()}.json.gz"
    )
