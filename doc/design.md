# Nursie design

## v0.1

Posts go in ${PROJDIR}/posts

Post filenames have the format YYYYMMDDTHHMMZ-TITLE.md
E.g., 20140212T0804Z-some-brilliant-post.md

(Assume a simple bash script that will "publish" draft posts by moving
them from ${PROJDIR}/drafts to ${PROJDIR}/posts, prepending their
filenames with the system date/time in basic ISO 4601 format.)

The build process goes through the posts directory and creates a seq
(prob. a set) of maps, each like this:

        {:fname "20140212T0804Z-some-brilliant-post.md"
         :pubdate "20140212T0804Z"
         :permalink "/2014/02/12/some-brilliant-post"
         :content [seq of clojure.xml nodes]
         :title "A really brilliant post" [text of h1, extracted from content]}

It then goes through this seq again, to add internal links, like this:

        {:prev ["2014/02/11/literay-genius" "A piece of literary genius"]
         :next ["2014/02/15/god-im-good" "God, I'm good"]
         :backlinks [["2014/03/21/still-brilliant" "Still brilliant"]
                     ["2015/02/12/year-on-still-great" "A year of greatness"]]}

Next it passes each map to a hard-coded enlive template (putting :title
and :content in the right places, inserting a TIME element for :pubdate,
and adding :prev, :next and :backlinks in the right place), and writes
the output to ${PROJDIR}/site/${:permalink}.html

It copies the output for the most recent post to
${PROJDIR}/site/index.html.

Finally, it copies the contents of ${PROJDIR}/assets to ${PROJDIR}/site
(e.g., ${PROJDIR}/assets/style/ -> ${PROJDIR}/site/style/, etc.)
