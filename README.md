# TunA: Tunable Query Optimizer for Web APIs and User Preferences

## What is TunA?
To answer queries many query processors combine different SPARQL sources, e.g., various knowledge bases or APIs. RESTful Web APIs are rarely the focus of those systems as they come with many limitations, such as not being able to process SPARQL queries. Additionally, most existing approaches optimize their query plans only for performance, even though users often have additional preferences regarding coverage, reliability, or currency. In addition, data is often provided with different levels of quality so that not all sources should be trusted equally.

TunA is a query optimizer that is able to combine RESTful Web APIs and RDF KBs in form of triple stores, tuning its (query) plans towards user preferences. Erroneous information from Web APIs is detected using a hierarchical agglomerative clustering. Our evaluation shows that TunA outperforms current state-of-the-art systems and is less vulnerable for erroneous information, even in settings where only unreliable sources are available

## How to run TunA?
In order to run TunA several configurations must be done first. First, clone the project and open it in an IDEA of your choice. Afterwards, three steps need to be done: (1) all local RDF databases that you want to query must be registered, (2) the function store (containing all alignments between the local used RDF databases and a set of APIs) must be configured and (3) you have to specify your queries and execute them.

All configurations of TunA are done in the config file `config.json`.

### Configure local databases and APIs
To add a database or change the location of a database, you must add or modify a JSON object of the following form in the database array, named `databases`, in the global configuration file `config.json`:

```
{
  "label": "example_dataset",
  "index": "C:\\path\\to\\tdb",
  "source": "C:\\path\\to\\dataset.nt"
}
```

The key `label` indicates the name of the database with which it can later be selected in order to make queries to it. The path to the triple store (in this case a triple database created by Apache Jena) or index is stored under `index`. It is also possible to specify a URL leading to the SPARQL endpoint of an RDF knwoledge base. Lastly, `source` stores the path to the source file (i.e., an .nt, .ttl, or other file).

The procedure is similar for APIs, because these must also be defined in the configuration file `config.json`. For this purpose, an entry of the following form must be created in the section called `apis`:

```
{
  "name": "Name of the API",
  "label": "apiLabel",
  "timeout": 500,
  "format": "json",
  "parameters": [
    {
      "name": "q",
      "type": "https://dblp.org/rdf/schema-2020-07-01#Publication",
      "relation": "http://www.wikidata.org/prop/direct/P356"
    }
  ],
  "url": "http://www.example.com/work/title?doi={q}"
}
```

The first two fields describe the name or label of the API. Here, the label also serves as a key and can be uniquely identified. The field `timeout` describes the time that must elapse between two requests in order to be allowed to make another request to the API. The timeout was introduced to be able to model rate limits, as APIs are often limited in the number of requests per second. The fields `format` and `url` represent the response format of the API and the URL that must be used to make requests.

The field `parameters` is used to specify the parameters of a call URL, e.g, `doi={q}` is a call parameter that needs to be set to a value of a DOI in order to successfully request information. The field `name` specifies the name of the placeholder of the parameter, that will be replaced with the actual value, e.g., a DOI. The `type` and `relation` field specify the type of the entity that must provide the input value, denoted by the relation specified in the field `relation`.

### Setting up a function store
The configuration file `config.json` can be used to define the storage location for one or multiple sets of function definitions (also denoted as function store).

```
{
  "functionstore": "res/functionstores/",
  ...
}
```

In the presented example above, the path to the function store is set to `res/functionstore/`. In the specified folder, several sub-folders can be created (see the example below) with different function definitions in order to model different function stores.

![function_defs_config](https://user-images.githubusercontent.com/120786910/210748228-89a0e146-d9d7-469b-9257-3f2a9a56ac8a.JPG)

In this example, three different function stores have been defined, namely `evalF1`, `evalF2` and `evalF3`. The function stores mentioned above contain a number of different function definitions, which map an API response to a knowledge base. In this case, a function definition is a JSON file, structured like the following example:

```
{
  "meta": {
    "database": "databaseName",
    "api": "apiLabel",
    "inputRelation": "http://www.wikidata.org/prop/direct/P356",
    "responseTime": 504,
    "responseProbability": 0.32
  },
  "alignments": [
    {
      "reliability": 0.75,
      "relation_path": [
        {
          "path": [
            "https://dblp.org/rdf/schema-2020-07-01#title"
          ]
        }
      ],
      "api_path": [
        "feed.entry.title"
      ]
    }
  ]
}

```

The first section, named `meta`, contains various information about the API and the response, modelled sub-graphs of the local database. The first fields `database` and `api` describe which API is modelled and with which local database it is aligned. The fields `responseTime` and `responseProbability` describe the average response time of the API and how likely it is that information can be returned for a requested entity. The field `inputRelation` describes the relation that points to a value (in this case a DOI) that must be used to request an API. In this example, a DOI (marked with the predicate P356) must be sent to the API in order to receive a response with information (i.e. title).

The second section is named `alignments` and contains for each path in an API response (typically in JSON or XML) a mapping to the corresponding relation or predicate of the local database. If a path cannot be mapped because, for example, the information is not available locally, no mapping is specified. In the above example, the path `feed.entry.title`, which leads to the title of a publication, is mapped to the relation `dblp:title`. This way the query processor knows that the function definition can be used to find a missing title of a publication.

### Running queries
By executing the class `TunaConsole`, the query processor TunA is started. The user is then asked to specify the database to which he wants to send a query, to specify the function store to be used to fill in missing information in the query result and to specify his query. The user formulates the query in a file and saves it in the path `res/queries/`. The example below shows that the user submits a query `queryName.sparql` to the database `databaseName` with funciton store `functionStoreName`.

```
___________               _____   
\__    ___/_ __  ____    /  _  \  
  |    | |  |  \/    \  /  /_\  \ 
  |    | |  |  /   |  \/    |    \
  |____| |____/|___|  /\____|__  /
                    \/         \/ 

-----------------------------
Data Management
-----------------------------
Select Database: databaseName
Select Function Store: functionStoreName
Query File: queryName.sparql

-----------------------------
User Preferences
-----------------------------
Time Limit (ms): 5000
Minimum Coverage: 0.7
Minimum Reliability: 0.8
```

Finally, before the query is executed, the user must specify their preferences (in terms of time limit, minimum required coverage, minimum required reliability). In the example above, the user has specified a maximum execution time of 5 seconds (measured in milliseconds). Furthermore, he requires a minimum coverage of 0.7 and a minimum reliability of 0.8.

## Where to find the data sets used for the evaluation?
For the evaluation we used as data sources the ones provided by [ETARA](https://github.com/anonresearcher123/ETARA) (an RDF version of dblp and data exports of the RESTful Web APIs from CrossRef, SciGraph and Semantic Scholar). Additionally, we created three tainted data sets, that contained erroneous data (i.e. incorrect titles, etc.) and removed information, e.g., titles and author names, from the dblp dataset.

[Download used data sets](https://www.dropbox.com/scl/fo/1t7gmh8bzbyieanacdl5a/h?dl=0&rlkey=66rejlyibvjxrb2z3hwsrji4v "Link to used datasets")
