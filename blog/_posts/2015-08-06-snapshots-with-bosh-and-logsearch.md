---
title: Snapshots with BOSH and Logsearch
description: "A tutorial/blog post on how to perform snapshots and restors with logsearch"
audience: writer, designer
author: "Alan Moran and Luke Rabczak"
author_github: "bonzofenix+cloud-dude"
tags: logsearch, elasticsearch
---

Logsearch relays on Elasticsearch for data persistence. There are situations in which you want to have your data replicated outside of your Elasticsearch cluster. For this Elasticsearch provides data snapshots and restore.

This requires a shared file system repository. Such as NFS, S3 repositories, HDFS repositories on hadoop, or Azure storage repositores.

Following two assumptions needs to be considered:

- You should have a Logsearch deployment in place.
- You should also have a NFS mount point (If you do not you can deploy one with the [nfs-boshrelease](https://github.com/compozed/nfs-boshrelease)). 

## Mounting NFS on your Elasticsearch cluster

Within the `nfs-boshrelease` there is a `nfs_mounter` job provided. This job will mount `/var/vcap/store` from the nfs server to `/var/vcap/nfs` on the elasticsearch nodes. To accomplish this you need to collocate the `nfs-boshrelease` mounter within the Logsearch deployment.  Also, you will need to provide this new job with the correct properties.

See Logsearch example stub below:

{% highlight yaml %}
---
meta:

name: logsearch
director_uuid: 12345-12345-12345-12345

releases:
- name: logsearch
  version: latest
################### Collocate NFS boshrelease ######################
- name: nfs
  version: 1.4
...

jobs:
- name: api
  templates:
  - name: elasticsearch
    release: logsearch
  - name: api
    release: logsearch
  - name: kibana
    release: logsearch
################### Adding the nfs_mounter job template ######################
  - name: nfs_mounter
    release: nfs
- name: elasticsearch_persistent
  templates:
  - name: elasticsearch
    release: logsearch
################### Adding the nfs_mounter job template ######################
  - name: nfs_mounter
    release: nfs
- name: elasticsearch_autoscale
  templates:
  - name: elasticsearch
    release: logsearch
################### Adding the nfs_mounter job template ######################
  - name: nfs_mounter
    release: nfs

{% endhighlight %}

## Prepare Elasticsearch cluster for creating repository

For the share filesystem implementation to work, all masters and data nodes need to know the path where snapshot repositories will be created. This is done through the `path.repo` Elasticsearch property. 

Logsearch provides a way to distribute extra Elasticsearch configuration options through the deployment manifest. 

See Logsearch example stub below:

{% highlight yaml %}
---
...

properties:
  ...
  elasticsearch:
    config_options: |
                   <%= 'path.repo: ["/var/vcap/nfs/shared"]'.gsub(/^/, '            ').strip # Doing this with ruby because spiff[0] does not merge quite well strings with ':' or 'foo.bar' format. %>
{% endhighlight %}

## Deploy kopf to Elasticsearch cluster

For monitoring and administrating Elasticsearch you can make things easier using the kopf plugin. Logsearch provides a solution for installing plugins. The only isssue is that it requires access to the internet to download them. You can use [elasticsearch-plugins-boshrelease](https://github.com/compozed/elasticsearch-plugins-boshrelease) to provide plugin sources offline.

First upload the `elasticsearch-plugins-boshrelease`:

{% highlight bash %}
    $ git clone https://github.com/compozed/elasticsearch-plugins-boshrelease.git && cd elasticsearch-plugins-boshrelease
    $ bosh upload release releases/elasticsearch-plugins/elasticsearch-plugins-1.yml
{% endhighlight %}

Now that elasticsearch-plugins-boshrelease is available, you need to install it up in the Elasticsearch api job. 

See Logsearch example stub below:
{% highlight yaml %}
---
releases:
...
######### Adds elasticsearch-plugins-boshrelease ##########
- name: elasticsearch-plugins
  version: 1
...
jobs:                                                                                                                                                                        
- name: api
  templates:
  - name: elasticsearch
    release: logsearch
  - name: api
    release: logsearch
  - name: kibana
    release: logsearch
  - name: nfs_mounter
    release: nfs
######### Adds elasticsearch-plugins template to the api job ##########
  - name: elasticsearch-plugins
    release: elasticsearch-plugins
  properties:
 ####### Installs kopf plugins #######
    elasticsearch:
      plugins:
      - kopf: file:///var/vcap/packages/elasticsearch-plugins/elasticsearch-kopf.zip
{% endhighlight %}


After generating new deployment manifest and performing a `bosh deploy` you should have Kopf running at [ https://LOGSEARCH_API_IP:9200/_plugin/kopf/#!/snapshot ]() and logsearch prepared for performing snapshots.

### 1 - Create snapshot with kopf

#### 1.1 Create Repository


![blog-img](/blog/uploads/2015-08-06-snapshots-with-bosh-and-logsearch/perform-snapshot-creation-1.png)

#### 1.2 Perform snapshot creation

![blog-img](/blog/uploads/2015-08-06-snapshots-with-bosh-and-logsearch/perform-snapshot-creation-2.png)

After a few minutes, the page needs to be refreshed to see the snapshot status either finished or in progress.

![blog-img](/blog/uploads/2015-08-06-snapshots-with-bosh-and-logsearch/perform-snapshot-creation-3.png)
 
### 2 - Import snapshot with kopf

To reproduce the import snapshot you need a new logsearch deployment. 

For trying out this you can perform `bosh delete deployment LOGSEARCH_NAME` and `bosh -n deploy`. This will give you clean and empty logsearch deployment.

Go ahead and recreate the repository again following 1.1 instructions.

#### 2.1 - Stop redis queue

You should not recover the current index. For a successful recovery of all indexes, you will need to stop the Redis queue and delete the current date index (if it exists).

{% highlight bash %}
  bosh stop queue 0
{% endhighlight %}

#### 2.2 Perform snapshot restore

Now your old snapshot should be available:

![blog-img](/blog/uploads/2015-08-06-snapshots-with-bosh-and-logsearch/perform-restore-1.png)

Importing indices is fairly easy: 

![blog-img](/blog/uploads/2015-08-06-snapshots-with-bosh-and-logsearch/perform-restore-2.png)

After the restoring proccess is complete and the Elasticsearch cluster balances the new restored indexes and shards, logsearch should have all the previos logs.

