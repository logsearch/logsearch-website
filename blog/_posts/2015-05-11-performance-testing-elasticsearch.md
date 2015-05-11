---
title: "Performance Testing Elasticsearch"
author: "Danny Berger"
author_github: "dpb587"
author_website: "http://dpb587.me/"
---

The [elastic](https://www.elastic.co/) team has made the Elasticsearch+Logstash+Kibana stack extremely easy to get started with. It's quick to start parsing logs into and easy to visualize that data with charts. When you need to start seriously scaling, things require a bit more planning - whether it's for higher availability, improved performance, or capacity planning.

One of the things [logsearch](https://github.com/logsearch/logsearch-boshrelease) tries to deal with is making sure critical components can scale out indefinitely. Logsearch is based on [BOSH](http://bosh.io/) which handles provisioning and imaging the servers to whatever your needs are at the moment. For example, if your deployment isn't quite keeping up with your data center's logging rates, you can just add another parser for several hundred more messages per second. Or, if elasticsearch isn't quite keeping up with your ingestion rates, you can add another few data nodes and further stripe your data.

Having scalable components is a fantastic first step - it's easy to just throw more servers at the situation and hope bottlenecks go away for a while. But, that only goes so far for so long. Recently we've started looking into ways we can quantifiably compare different scaling configurations to find and validate the next change to improve our own deployments. Using [logsearch-shipper](https://github.com/logsearch/logsearch-shipper-boshrelease) in our deployed performance test environments we're able to record all sorts of host and custom deployment statistics that we can correlate and compare afterwards.

For this post, we've gone through a few, simple deployment scenarios to identify various bottlenecks from the ingestion side of things to range from throughputs of 300 to 5000 messages per second.

1. Standalone/micro ELK (everything on one server)
1. Each component having their own server (ingestor/queue + parser + elasticsearch)
1. Using multiple parser servers
1. Using multiple (more powerful) parser servers
1. Using multiple elasticsearch servers
1. Using multiple elasticsearch servers w/ SSD disks

We wrote a test environment for ourselves - something which goes through reproducible steps for our various deployment manifests that we want to test. You can learn more about the environment from the [repository](https://github.com/logsearch/bosh-performance-tests). In addition to our core scripts, the core manifests and configurations from this article are committed there as well in case you want to run them with your own specs.

For all of these tests, we're running on [AWS](http://aws.amazon.com/), using logsearch [v19](http://bosh.io/releases/github.com/logsearch/logsearch-boshrelease?version=19), re-ingesting some of our previously archived logs, ingesting for at least a solid hour, and running at least 2 trials to verify consistency. We'll summarize the settings we use here, but you can always check out the full manifests committed to the repository if you're interested in the raw details. We've added our own internal, org-specific logstash filters and elasticsearch mappings for better, real world tests with the historical log messages.


## MicroELK

For this test, we're simply running all of the services on a single box -- a popular way people get started with ELK. Here we'll run everything on a `c4.large` (2 core, 8 ECU, 3.75 GB).

In the best trial, we averaged **316 messages /second**, 19,019 /minute. The CPU is the most obvious bottleneck with it constantly being maxed out the whole time.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/A-standalone/cpu.png)


## Component Servers

After outgrowing a single server, the next step is often to try and split up the services onto separate servers. It at least demonstrates the distinct CPU and RAM requirements. For simplicity, we continued using `c4.large` here for all the servers.

In the best trial, we averaged **406 messages /second**, 24,414 /minute. That's a tiny bit more throughput now that everything is running in its own environment, but the CPU on the parser still appears to be maxed out the whole time.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/B-components/cpu.png)


## More Parsers

Since the parser appears to be the bottleneck, we can try horizontally scaling it. Here we'll add 3 more parser servers for a total of 4.

In the best trial, we more than quadrupled throughput to **1735 messages /second**, 104,103 /minute. Interestingly, we still have not yet reached the indexing limitations of elasticsearch though - the CPU of the parsers still appears to be our bottleneck.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/C-parsers/cpu.png)

We can continue adding capacity to parsers, although this time we'll vertically scale them. Instead of `c4.large`, we'll start using `c4.2xlarge`.

In the best trial, we averaged **3882 messages /second**, 232,944 /minute. Finally, the parser CPU no longer appears to be the bottleneck since CPU is only peaking around 45%, and the elasticsearch CPU is now hovering around 100%.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/D-4xparsers/cpu.png)

## More Elasticsearch

As you probably know, elasticsearch has excellent support for easily sharding and distributing its data. In this default setup, we have been using 4 shards of data, but all of that data resides on a single server and disk. Here we add three more nodes for elasticsearch to store data on. This will roughly mean each node will have one primary shard and one replica (up until now, no replicas were being used).

In the best trial, we averaged **4396 messages /second**, 263,795 messages /minute, slightly up from our previous test. Since our last test, we did effectively double the amount of disk I/O and storage we're using since this is first time a replica can be assigned for each of our 4 primary shards.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/E-elasticsearch/cpu.png)

## Other Configuration

At this point we've quickly grown to a very capable cluster. At some point in performance testing, you would want to adjust a few more configuration options. Here, we can try switching to SSD disks, extending the indices' refresh interval to 10 seconds, and raising the threshold that we start throttling disk operations.

In the best trial, we averaged **4884 messages /seconds**, 293,097 messages /minute.

![CPU](/blog/uploads/2015-05-11-performance-testing-elasticsearch/F-config/cpu.png)

As an example of the configuration effects, notice the difference between the default throttle (blue) and the customized threshold (black):

![Throttle Compared](/blog/uploads/2015-05-11-performance-testing-elasticsearch/F-config/throttle-compare.png)


## Recap

So we've gone through the simple process of scaling from a standalone logsearch ELK deployment indexing just over 300 messages /second to one which can be indexing nearly 5000 messages /second. This method of scaling - just throwing more hardware in the right places - is fairly easy and effective. For more finely-tuned and cost-effective clusters, you'll want to start with smaller setups and tweaking elasticsearch options to find what works best for your requirements before applying it to a full cluster. There's an article on [elastic.co](http://www.elastic.co/guide/en/elasticsearch/guide/master/indexing-performance.html) which documents some great starting points. These scenarios were also only testing the indexing side of things - fully timed, replayable search loads are something we haven't fully been performance testing yet.

![Rate Compared](/blog/uploads/2015-05-11-performance-testing-elasticsearch/rate-compare.png)

This approach of performance testing different configurations is turning out to be a great tool for us in evaluating which settings we want to maintain. BOSH provides us the highly reproducible environments, logsearch-shipper lets us capture all the metrics we'll want to compare, logsearch becomes our test subject, and some release-specific scripts provide the repeatable steps for testing our particular deployments.

## What next?

We'd love to get to the point where we run a daily performance test using yesterday's data as a way of catching any performance regressions before they make it to a production environment AND validating that our archiving strategy can be restored.

If you're interested in this and would like to get involved, we'd love to hear from you - raise an issue on [logsearch/bosh-performance-tests](https://github.com/logsearch/bosh-performance-tests) so we can discuss it further.


