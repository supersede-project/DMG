# synaptic

The SYNAPTIC library consists of components for pre-processing data (e.g., tokenization), extracting relevant features and for 'sentiment' and 'type' classification for German. Training and classification operations are accessible via the SYNAPTIC Application Program Interface (API). In addition, a Command Line Interface (CLI) is provided for convenience of experiments and training.

## Getting started

These instructions will get you a copy of the project up and running on your local machine.

### Prerequisites

- Java 1.8 or later
- Apache Maven (http://maven.apache.org) (required by SYNAPTIC API)

### How to get the code

Two different distributions of SYNAPTIC are provided: Jar Distribution and Java Source Distribution; the first distribution can be used for running SYNAPTIC from CLI, while the second distribution can be used when you want to use SYNAPTIC as a library from your java code.

#### Jar Distribution (CLI)

This is a jar file containing all the Java code for training and testing. It can be download from this address: 

https://github.com/rzanoli/synaptic/releases/download/v1.0/synaptic-1.0-SNAPSHOT-jar-with-dependencies.jar

#### Java Source Distribution (API)

The zip file of the Maven project containing the project source code:

https://github.com/rzanoli/synaptic/archive/v1.0.zip

### Dataset format

The training dataset to train the classifier is a tsv file that contains the following fields separeted by a tabular space:

ID \t start/end time \t sentiment \t type \t Content

e.g.,
```
ID	start/end time	sentiment	type	Content
#1	2016	negative	bug report	Der Kessel funktioniert schlecht; Das Befehlsmodul antwortet nicht
#2	2015	neutral	other	Dieser Kessel funktioniert gut, aber die Steuerung könnte schneller sein
```

As regards the dataset to annotate, the classifier accepts in input single texts that are in raw text format, e.g.,

```Der Kessel funktioniert schlecht; Das Befehlsmodul antwortet nicht```

## CLI Instructions

After getting the Jar Distribution as explained above, you are ready for training the classifier on a dataset and annotating new examples. These instructions are valid for both the 'sentiment' classifier and 'type' classifier. In the rest of this section we will only report instructions for the 'sentiment' classifier but they remain valid for the other classifier: it is sufficient to change the package name from 'sa' to 'tc' and the classifier's name prefix from 'Sentiment' to 'Type' (i.e., sa.SentimentLearn --> tc.TypeLearn, sa.SentimentClassify --> tc.TypeClassify).

### Installation

Save the SYNAPTIC jar file that you have just downaloded into your working directory.

### Training

From the working directory, run the following command:

```> java -cp synaptic-1.0-SNAPSHOT-jar-with-dependencies.jar eu.fbk.ict.fm.nlp.synaptic.classification.sa.SentimentLearn -f datasetFileName -m modelFileName```

Where:
- datasetFileName is the name of the file containing the training dataset for training the classifier 
- modelFileName is the file name of the model to generate

Produced files:
 	
- modelFileName				the trained model
- modelFileName.features.index		the features index
- modelFileName.labels.index		the labels index
- datasetFileName.sa.token		the pre-processed dataset
- datasetFileName.sa.token.vectors	the features vectors

the generated files with prefix 'modelFileName' will be used in the next phase for annotating new examples while the files with prefix 'datasetFileName' are produced for training the classifier and the saved for debugging purposes only.


### Classification

From your working directory, run the following command:

```> java -cp synaptic-1.0-SNAPSHOT-jar-with-dependencies.jar eu.fbk.ict.fm.nlp.synaptic.classification.sa.SentimentClassify -c content -m modelFileName```

Where: 
- content is the text string to classify, e.g., "Der Kessel funktioniert schlecht"
- modelFileName is the model generated during the classifier training phase (it consists of all the 3 files generated during the training phase: modelFileName, modelFileName.features.index, modelFileName.labels.index that have to stay in the same directory).


### Evaluation

From the working directory, run the following command to perform 10-Fold Cross-Validation and evaluate the classifier:

```> java -cp synaptic-1.0-SNAPSHOT-jar-with-dependencies.jar eu.fbk.ict.fm.nlp.synaptic.classification.sa.SentimentLearn -f datasetFileName -m modelFileName -c```

Where:
- datasetFileName is the name of the file containing the training dataset for evaluating the classifier 
- modelFileName is the file name of the model (it is requested to produce the features but the model will not be generated)
- 'c' to perform cross validation


## API Instructions

SYNAPTIC has been developed as a Maven project and after getting its java Source Distribution as explained above, you need to install its maven artifact into your local maven repository (i.e., m2). Then you have to put the following dependency into the project file (i.e., pom.xml) of your java project.

```
<dependency>
	<groupId>eu.fbk.ict.fm.nlp</groupId>
	<artifactId>synaptic</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

### Installation

Copy the project that you have cloned from github into your working directory, and then from that directory run the following maven command to install the SYNAPTIC artifact into your maven local repository:

```
> mvn install
```

The SYNAPTIC API is now available in your favourite java IDE (e.g., Eclipse)

### Training

The following piece of code can be used to train the classifier:

```java
try {
    SentimentLearn sentimentLearn = new SentimentLearn();
    sentimentLearn.run(datasetFileName, modelFileName);
} catch (Exception ex) {
    System.err.println(ex.getMessage());
}
```

Where: 
- datasetFileName is the training dataset for training the classifier 
- modelFileName is the model generated during the classifier training phase

### Classification

Once the classifier has been trained ypu can use the generated model to annotate new examples; the following piece of code does this operation:

```java
try {
    SentimentClassify sentimentClassify = new SentimentClassify(modelFileName);
    String[] annotation = sentimentClassify.run(content); 
    String label = annotation[0]; // the predicted label
    String score = annotation[1]; // and its score
    System.out.println("predicted label:" + label + " score:" + score);
} catch (Exception ex) {
      System.err.println(ex.getMessage());
}
```

### Evaluation

The following piece of code can be used to perform 10-Fold Cross-Validation and evaluate the classifier:

```java
try {
    SentimentLearn sentimentLearn = new SentimentLearn();
    sentimentLearn.setCrossValidation(1);
    sentimentLearn.run(datasetFileName, modelFileName);
} catch (Exception ex) {
    System.err.println(ex.getMessage());
}
```

Where: 
- datasetFileName is the training dataset for training the classifier 
- modelFileName is the model (it is requested to produce the features but the model will not be generated)



### Example of calling SYNAPTIC API from java code

```java
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import eu.fbk.ict.fm.nlp.synaptic.classification.sa.SentimentClassify;
import eu.fbk.ict.fm.nlp.synaptic.classification.sa.SentimentLearn;

/**
*
* This class shows an example on how the SYNAPTIC API can be used for training the semantic classifier on
* a given dataset and then use the produced classifier for classifying a new dataset. The training dataset is a tsv file
* in the format as described by the class FileTSV (it is included in the project), while the dataset to annotate contains
* raw texts each of them on a separate line.
*
*/
public class LearnAndClassifyTest {

	private static String trainDataset = "train_dataset.tsv"; // training dataset
	private static String testDataset = "test_dataset.tsv"; // the dataset to annotate
	private static String model = "/tmp/dataset.sa.model";

        /**
	* Trains the classifier
	*/
	public static void Learn() throws Exception {

		SentimentLearn semanticLearn = new SentimentLearn();
		semanticLearn.run(trainDataset, model);

	}

        /**
	* Classifies by using the generated model
	*/
	public static void Classify() throws Exception {

		BufferedReader in = null;

		try {
			SentimentClassify semanticClassify = new SentimentClassify(model);
			File file = new File(dataSet);
			in = new BufferedReader(new InputStreamReader(new FileInputStream(testDataset), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				String[] annotation = semanticClassify.run(str);
				String label = annotation[0]; //predicted label
				String score = annotation[1]; //and its score
				System.out.println("predicted label:" + label + " score:" + score);
			}
		} catch (Exception ex) {
			throw(ex);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
					throw(e);
				}
		}

	}

	public static void main(String[] args) {

		try {
			LearnClassifyTest.Learn();
			LearnClassifyTest.Classify();
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

	}

}
```

## Authors

## License

This project is licensed under the Apache License v2.0.
