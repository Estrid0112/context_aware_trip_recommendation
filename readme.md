### Dependency
```
- lpsolve5.5 : Please install lpsolve following the instructions in http://lpsolve.sourceforge.net/5.5/
- Java SDK 11
```


### Install the program

```
mvn install:install-file -Dfile=libs/lpsolve55j.jar -DgroupId=lpsolve -DartifactId=java-wrapper -Dversion=5.5 -Dpackaging=jar
mvn install
```

### Run the program
```
java -jar target/context_aware_trip_recommendation-1.0-SNAPSHOT-jar-with-dependencies.jar config/configuration.properties
```

### Datasets
#### https://sites.google.com/site/limkwanhui/datacode?authuser=0:

##### "data/userVisits-City.csv" contains all users' visits:

- photoID: identifier of the photo based on Flickr.
- userID: identifier of the user based on Flickr.
- dateTaken: the date/time that the photo was taken (unix timestamp format).
- poiID: identifier of the place-of-interest (Flickr photos are mapped to POIs based on their lat/long).
- poiTheme: category of the POI (e.g., Park, Museum, Cultural, etc).
- poiFreq: number of times this POI has been visited.
- seqID: travel sequence no. (consecutive POI visits by the same user that differ by <8hrs are grouped as one travel sequence).


##### "data/POI-City.csv" contains all POIs in the City:

- poiID: identifier of the POI.
- poiName: actual name of the POI.
- lat/long: latitude/longitude of the POI
- theme: category of the POI


##### "data/costProfCat-CityPOI-all.csv" contains the travelling costs and profit scores for each pair of POI

 - from: poiID of the starting POI.
 - to: poiID of the destination POI.
 - cost: distance (metres) between the starting POI (from) to the destination POI (to).
 - profit: popularity of the destination POI (to), based on number of POI visits
 - theme: category of the POI (e.g., Park, Museum, Cultural, etc).


##### "config/configuration.properties" contains the set of parameters of the model

 - dataDir: the path to input data
 - city: the city name
 - minSeqPerUser: in preprocessing, only keep the users with at least "minSeqPerUser" travel sequences (since we are doing leave-one-out cross-validation, this parameter is set as 2) 
 - minSeqLen: in preprocessing, only keep the sequences with at least "minSeqLen" POIs (since we presume starting and ending POI, this parameter is set as 3)
 - numEpoch: maximum training iterations 
 - numDim: dimension size of user and POI latent representations
 - lr: learning rate
 - regWeight: regularization weight
 - contextWeight: weight of POI contextual similarity when computing profit scores of trips 
 - metrics: evaluation metrics - (recall;precision;F1 equal the metrics in Table IV) (recall_2;precision_2;F1_2 equal the metrics in Table III)


