---
title: "Creating a Blog Post"
---

We use [Jekyll](http://jekyllrb.com/) for managing the site content, and they have a built-in concept of blog posts that
we utilize. First, you should be familiar with [Jekyll's "Writing Posts"](http://jekyllrb.com/docs/posts/) documentation
page before reviewing the specific conventions we use below.


## Files

Each post should be written in its own file within the `blog/_posts` directory. Your file name should use the following
naming convention:

    {yyyy-mm-dd}-{post-title}.md

The `yyyy-mm-dd` should be the date your post is published and the `post-title` should be the dash-form of the title of
your post.


## Front Matter

When writing posts, at a minimum, you must include the following front matter...

 * `title` - the title of your post
 * `description` - a very short, one sentence summary of the post (used in meta and post summaries)
 * `author` - your name, as author of the content
 * `author_github` - your GitHub username, as author of the content

The website acts as a primary authority for the documentation of our various projects and repositories. When working
with the website repository, you will typically want local copies of those repositories.


## Images

If you need to include images with your post, create a directory in `blog/uploads` named after your filename and commit
your files there. Then you can reference them in your post as `/blog/uploads/2014-01-01-happy-new-year/fireworks.jpg`.
You must ensure your images are of reasonable size (e.g. do not commit raw images or screenshots - resize them to be
under 960px wide and well under 1MB in size).

To reference images in [Markdown](http://daringfireball.net/projects/markdown/), you will do something like:

    # include your image in the site
    $ mkdir blog/uploads/2014-01-05-the-new-dashboard
    $ cp ~/Desktop/sample-dashboard-640x480.jpg blog/uploads/2014-01-05-the-new-dashboard/
    
    # write your post
    $ vim blog/_posts/2014-01-05-the-new-dashboard.md
    
    # reference your image with the image syntax
    # ![The New Dashboard](/blog/uploads/2014-01-05-the-new-dashboard/sample-dashboard-640x480.jpg)
    
    # add your image and post content to the repo
    $ git add -A blog/uploads/2014-01-05-the-new-dashboard blog/_posts/2014-01-05-the-new-dashboard.md
