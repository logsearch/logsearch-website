---
title: "Managing the Upstream /docs/"
---

The website acts as a primary authority for the documentation of our various projects and repositories. When working
with the website repository, you will typically want local copies of those repositories.

## Fetching Upstream Docs

The [`bin/reload-upstream`][1] script is responsible for downloading a copy of the referenced repositories and
including them in the website structure. The repositories will be cached in `_upstream` and the repositories' `docs`
directories will be symlinked into the website's `/docs/` directory.

To run, simply execute the script. It is safe to run many times, and each time it will run a `git pull --ff-only` to
ensure you have the latest changes.

    $ ./bin/reload-upstream
    --> reloading docs/boshrelease
    Cloning into '_upstream/https---github-com-logsearch-logsearch-boshrelease-git'...
    remote: Counting objects: 2327, done.
    remote: Compressing objects: 100% (38/38), done.
    remote: Total 2327 (delta 6), reused 0 (delta 0)
    Receiving objects: 100% (2327/2327), 5.08 MiB | 288.00 KiB/s, done.
    Resolving deltas: 100% (1160/1160), done.
    Checking connectivity... done.
    --> reloading docs/shipper-boshrelease
    ...snip...

By default, the script will symlink the project directories into the website. This makes it easier if you want to make
minor changes to project documentations, switch to its `_upstream` checkout, and commit your changes. Alternatively,
you can specify `copy` as the first argument to avoid the symlinks, but be aware any changes will be overwritten the
next time you run `reload-upstream`.

### Reusing Existing Repositories

If you prefer, you can manually manage the `/docs/` directory yourself instead of using `reload-upstream`. This may be
simpler if you want to reuse your existing repositories and project-specific development environments. For example...

    $ cd docs
    $ ln -s boshrelease ~/Projects/logsearch/logsearch-boshrelease/docs


## Adding Upstream Docs

As we add new repositories with docs, we need to keep `reload-upstream` updated. The script uses the following function
and its arguments:

    fetchdocs(
        # where to install docs within this repo
        "docs/boshrelease"
        
        # the repository URL to use
        "https://github.com/logsearch/logsearch-boshrelease.git"
        
        # the repository path to the docs
        "docs"
    )

Review the end of `reload-upstream` to see the current repository references and examples on usage.


 [1]: https://github.com/logsearch/logsearch-website/blob/master/_build/bin/reload-upstream
