<link href="../css/markdown.css" type="text/css" rel="stylesheet"/>

# Machine Learning

{toc:h1-h6}
<a name="C-1"></a>
#Chapter 1.
<a name="Interaction_between_features"></a>
<i><font size="3" color="0x0000ff" face="Times">Interaction between features</font></i>

--One fascinating and multi-faceted aspect of features is that they may interact in various ways. Sometimes such interaction can be exploited, sometimes it can be ignored, and sometimes it poses a challenge. We have already seen an example of feature interaction when we talked about Bayesian spam filtering. Clearly, if we notice the term 'Viagra' in an e-mail, we are not really surprised to find that the e-mail also contains the phrase 'blue pill'. Ignoring this interaction, as the naive Bayes classifier does, means that we are overstimating the amount of information conveyed by observing both phrases in the same e-mail. Wether we can get away with this depends on our task: in spam email classification it turns out not to be a big problem, apart from the fact that we may need to adapt the decision threshold to account for this effect.

&nbsp;&nbsp;&nbsp;&nbsp;We can observe other examples of feature interaction in <a href=#Table1.4>Table 1.4</a>. Consider the features 'grad' and 'real', which assess the extent to which models are of the grading kind, and the extent to which they can handle real-valued features. You may observe that the values of these two features differ by at most 1 for all but one model. Statisticians say that these features are positively correlated (see <a href=#Background1-3>Background 1.3</a>). Another pair of positively correlated features is 'logic' and 'disc', indicating logical models and the ability to handle discrete features. We can also see some negatively crrelated features, where the value of one goes up when the other goes down: this holds naturally for 'split' and 'grad', indicating whether models are primarily grouping or grading models; and also for 'logic' and 'grad'. Finally pairs of uncorrelated features are 'unsup' and 'multi',standing for unsupervised models and the ability to handle more than two classes; and 'disc' and 'sup', the latter of which indicates supervised models.<br>
&nbsp;&nbsp;&nbsp;&nbsp;In classification, features may be differently correlated depending on the class. For instance, it is conceivable that for somebody whose last name is Hilton and who works for the Paris city council, e-mails with just word 'Paris' or just the word 'Hilton' are indicative of ham, whereas e-mails with both terms are indicative of spam. Put differently, within the spam class these features are positively correlated, while within the ham class are negatively correlated. In such a case, ignoring these interactions will be detrimental for classification performance. In other cases, feature corelations may obscure the true model -- we shall see examples of this later in the book. On the other hand, feature corelation sometimes helps us to zoom in on the relevant part of the instance space.

<a name="Background-1-3"></a>
<hr>

<font color="red" face="Times">*Random variables*</font> descrive possible outcomes of a random process. They can be either discrete (e.g., the possible outcomes of rolling a die are {1,2,3,4,5,6}) or continuous (e.g., the possible outcomes of measuring somebody's weight in kilograms). Random variables do not need to range over integer or real numbers, but it does make the mathematics quite a bit simpler so that is what we assume here.<br>
If X is a discrete random variable with probability distribution P(X) then the <font color="red" face="Times">*expected value*</font> of X is <img src="img/1.png" align="middle">. For instance, the expected value of tossing a fair outcome. For a continuous random variable we need to replace the sun with an integral, and the probability distribution with a probability density function: <img src="img/2.png" align="middle">. The idea of this rather abstract concept is that if we take a sample <img src="img/3.png" align="middle"> of outcomes of random process, the expected the <font color="red" face="Times">*sample mean*</font> <img src="img/4.png" align="middle"> to be -- this is celebrated <font color="red" face="Times">*law of large numbers*</font> first proved by Jacob Bernoulli in 1713. For this reason the expected value is often called the <font color="red" face="Times">*population mean*</font>, but it is important to realise that the latter is a theoretical value, while the sample mean is an empirical <font size="4" color="red" face="Time">*estimate*</font> of that theoretical value.

The expectation operator can be applied to functions of random variables. For instance, the (population) <font size="4" face="Times" color="red">*variance*</font> of a discrete random variable is defined as <img src="img/5.png" align="middle"> -- this measures the spread of the distribution around the expected value.<br>
Notice that<br>
<img src="img/6.png">


We can similarly define the <font color="red" face="Times">*sample variance*</font> as <img src="img/7.png" align="middle">,which decomposes as <img src="img/8.png" align="middle">. You will sometimes see the sample variance defined as <img src="img/9.png" align="middle">: dividing by *n-1* rather than *n* results in a slightly larger estimate, which compensates for the fact that we are calculating the spread around the sample mean rather than the population mean.<br>
The (population) <font color="red" face="Times">*covariance*</font> between two discrete random variables X and Y is defined as <img src="img/10.png" align="middle"> The variance of X is a special case of this, with Y = X. Unlike the variance, the covariance can be positive as well as negative. Positive covariance means that both variables tend to increase or decrease together; negative covariance means that if one variable increases, the other tends to decrease. If we have a sample of pairs of values of X and Y, <font color="red" face="Times">*sample covariance*</font> is defined as <img src="img/11.png" align="middle">. By dividing the covariance between X and Y by <img src="img/12.png"> we obtain the <font color="red" face="Times">*correlation coefficient*</font>, which is a number between -1 and +1.
<hr>

<font color="blue" face="Times">**Background 1.3.**</font>  Expectations and estimations

[TOC](#TOC)

There are other ways in which features can be related. Consider the following three features can be true or false of a molecular compound:

	1. it has a carbon in a six-membered aromatic ring:
	2. it has a carbon with a partial charge of -.13
	3. it has a carbon in a six-membered aromatic ring with a partial charge of -.13
	
We say that the third feature is more <font color="red" face="Times">*specific*</font>(or less <font color="red" face="Times">*general*</font>) than the other two, because if the third feature is true, then so are the first and the second. However, the converse does not hold: if both first and second feature are true, the third feature may still be false (because the carbon in the six-membered ring may not be the same as the one with a partial charge of -.13). We can exploit these relationships when searching for features to add to our logical model. For instance, if we find that the third feature is <fr>true of negative example</fr> that we're trying exclude, then there is no point in considering the more general first and second features, because they will not help us in excluding the negative either. Similarly, if we find that the first is false of a particular positive we're trying to include, there is no point in considering the more specific third feature instead. In other words, these relationships help us to structure our search for predictive features. 

[TOC](#TOC)

<a name="C1-4"></a>
### 1.4 Summary and outlook

My goal in this chapter has been to take you on a tour to admire the machine learning landscape, and to raise your interest sufficiently to want to read the rest of the book. Here is a summary of the things we have been looking at.

<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> Machine learning is about using the right features to build the right models that achive the right tasks. These tasks include: binary and multi-class classification, regression, clustering and descriptive modelling. Models for the first few of these tasks are learned in a supervised fashon requiring labelled training data. For instance, if you want to train a spam filter using machine learning, you need a training set of e-mails labelled spam and ham. If you want to know how good the model is you also need labelled test data that is distinct from the training data, as evaluating your model on the data it was trained on will paint too rosy a picture: a test set is needed to expose any overfitting that occurs.
</blockquote>

<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> Unsupervised learning, on the other hand, works with unlabelled data and so there is no test data as such. For instance, to evaluate a particular partition of data into clusters, one can calculate the average distance from the clusters, one can calculate the average distance from the center. Other forms of unsupervised learning include learning assosiations (things that tend to occur together) and identifying hidden variables such as film genres. Overfitting is also a concern in unsupervised learning: for instance, assigning each data point its own cluster will reduce the average distance to the cluster center to zero, yet is clearly not very useful.
</blockquote>

<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> On the output side we can distinguish between predictive models whose outputs involve the target variable and descriptive models which identify interesting structure in the data. Often, predictive models are learned in a supervised setting while descriptive models are obtained by unsupervised learning methods, but there are also examples of supervised learning of descriptive models(e.g., subgroup discovery which aims at identifying regions with an unusual class distribution) and unsupervised learning of predictive models(e.g., predictive clustering where the identified clusters are interpreted as classes).
</blockquote>
<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> We have loosely divided machine learning models into geometric models, probailistic models and logical models. Geometric models are constructed in Cartesian spaces, using geometric model is the basic linear classifier, which constructs a decision plane orthogonal to the line connecting the positive and negative centers of mass. Probablistic models view learning as a process of reducing uncertainly using data. For instance, a Bayesian classifier models the posterior distribution P(X \| Y) (or its counterpart, the likelihood function P(X\|Y)) which tells me the class distribution Y after observing the feature vlues X. Logical models are the most 'declarative' of the three, employing if-then rules built from logical conditions to single out homogeneous areas in instance space.
</blockquote>
<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> We have also introduced a introduced a distinction between grouping and grading models. Grouping models divide the instance space into segments which are determined at training time, and hence have a finite resolution. On each segment, grouping models usually fit a very simple kind of model, such as 'always predict this class'. Grading models fit a more global model, graded by the location of an instance in instance space (typically, but not always, a Cartesian space). Logical models are typical examples of grouping models, while geometric models tend to be grading in nature, although this distinction isn't clear-cut. While this sounds very abstract at the moment, the distinction will become much clearler when we discuss coverage curves in the next chapter.
</blockquote>
<blockquote>
<img src="img/finger.jpg" style="width: 15px;" align="top"/> Last but not least, we have discussed the role of features in machine learning. No model can exist without features, and sometimes a single feature is enough to build a model. Data doesn't always come with ready-made features. Because of this, machine learning is often an iterative process: we only constructed the model, and if the model doesn't perform satisfactorily we need to analyse its performance to understand in what way the features need to be improved.
</blockquote>

###<fb>What you'll find in the rest of the book</fb>
In the next nine chapters, we will follow the structure laid out above, and look in detail at
> <img src="img/finger.jpg" style="width: 15px;" align="top"/> machine learning tasks in [Chapter 2](#C-2) and [3](#C-3);

> <img src="img/finger.jpg" style="width: 15px;" align="top"/> logical models: concept learning in [Chapter 4](#C-4), tree models in [Chapter 5](#C-5) and rule models in[Chapter 6](#C-6);

> <img src="img/finger.jpg" style="width: 15px;" align="top"/> geometric models: linear models in [Chapter 7](#C-7) and distance-based models in [Chapter 8](#C-8);

> <img src="img/finger.jpg" style="width: 15px;" align="top"/> probabilistic models in [Chapter 9](#C-9); and

> <img src="img/finger.jpg" style="width: 15px;" align="top"/> features in [Chapter 10](#C-10).

[Chapter 11](#C-11) is devoted to techniques for training 'ensembles' of models that have certain advantages over single models. In [Chapter 12](#C-12) we will consider a number of methods for what machine learners call 'experiments', which involve training and evaluating models on real data. Finally, in the [Epilogue](#Epilogue) we will wrap up the book and take a look ahead.

[TOC](#TOC)

<a name="C-2"></a>
#CHAPTER 2
<br>
<hr>
<font face="Times" size="6" color="blue">Binary classification and related tasks</font>
<hr>

<font face="Times" size="5" color="blue">I</font><fb>N THIS CHAPTER</fb>  and the next we take a bird's-eye view of the wide range of different tasks that can be solved with machine learning techniques. 'Task' here refers to whatever it is that machine learning is intented to improve performance of (recall the definition of machine learning), for example, e-mail spam recognition. Since this is a classification task, we need to learn an appropriate classifier from training data. Many different types of classifiers exist: linear classifiers, Bayesian classifiers, distance-based classifiers, to name a few. We will refer to these different types as models; they are subject of [Chapter 4-9](#TOC). Classification is just one of a range of possible tasks for which we can learn a model: other tasks that will pass the review in this chapter are class probability estimation and ranking. In the next chapter we will discuss regression, clustering and descritive modelling.  For each of these tasks we will discuss what it is, what variants exist, how performance at the task could be assessed, and how it relates to other tasks. We will start with some general notation that is used in this chapter and throughout the book (see [Background 2.1](#B2-1) for the relevant mathematical concepts).

The objects of interest in machine learning are usually referred to as <fr>*instances*</fr>. The set of all possible instances is called the <fr>*instance space*</fr>, denote <img src="img/x.png" align="middle"> in this book. To illustrate, <img src="img/x.png" align="middle"> could be the set of all possible e-mails that can be written using the Latin alphabet. We furthermore distinguish between the <fr>*label space*</fr> ![L](img/L.png) and the <fr>*output space*</fr> ![Y](img/Y.png). The label space is used in supervised learning to label the examples. In order to achive the task under consideration we need a <fr>*model*</fr>: a mapping from the instance space to the output space. For instance, in classification the output space is a set of classes, while in regression it is the set of real numbers. In order to learn such a model we require a <fr>*training set*</fr> *Tr* of <fr>*labelled instances*</fr> (*x*, *l*(*x*)), also called <fr>*examples*</fr>, where *l*:![X](img/x.png) -> ![L](img/L.png) is a labelling function.

Based on this terminology and notation, and concentrating on supervised learning of predictive models for the duration of the chapter, [Table 2.1](#T2-1) distinguishes a number of specific scenarios. The most commonly encountered machine learning scenario is where the label space coincides with the output space. That is ,![Y](img/Y.png) = ![L](img/L.png) and we are trying to learn an approximation ![13](img/13.png) to the true labelling function *l*, which is only known through the labels it assigned to the training data. This scenario covers both classification and regression. In cases where the label space and the output space differ, this usually servers the purpose of learning a model that outputs more than just a label -- for instance, a score for each possible label. In this case we have ![14](img/14.png), with ![15](img/15.png) the number of labels.

Matters may be complicated by <fr>*noise*</fr>, which can take the form of <fr>*label noise*</fr> -- instead of *l*=*l*(*x*) we observe some corrupted label *l*' -- or <fr>*instance noise*</fr> -- instead of *x* we observe an instance *x*' that is corrupted in some way. One consequence of noisy data is that it is generally not advisable to try to match the training data exactly, as this may lead to overfitting the noise. Some of the labelled data is usually set aside for evaluating or testing a classifier, in which case it is called a <fr>*test set*</fr> and denoted by *Te*. We use superscripts to restrict training or test set to a particular class: *e.g.,*  ![16](img/16.png) is the set of positive test examples, and ![17](img/17.png) is the set of negative test examples.

The simplest kind of input space arises when instances are described by a fixed number of <fr>*features*</fr>, also called attributes, predictor variables, explanately variables or independent variables. Indicating the set of values or <fr>*domain*</fr> of feature by ![18](img/18.png), we then have that ![19](img/19.png), and thus every instance is a *d-vector* of feature values. In some domains the features to use readily suggest themselves, whereas in other domains they need to be constructed. For example, in the spam filter example in the <fb>Prologue</fb> we constructed a large number of features, one for each word in a vocabulary, counting the number of occurrences of the word in the e-mail. Even when features are given explicitly we often want to transform them to maximise their usefulness for the task at hand. We will discuss this in considerable detail in [Chapter 10](#C-10).

<hr>
<a name="B2-1"></a>

We briefly review some important concepts from discrete mathematics. A <fr>*set*</fr> is a cpllection of objects, usually of the same kind (*e.g.,* the set of all natural numbers *N* or the set of real numbers *R*). We write *x* ∈ *A* if *x* is an element of set A, and A ⊆ B if all elements of A are also elements of B (this includes the possibility that A and B are the same set, which is equivalent to A ⊆ B and B ⊆A). The <fr>*intersection*</fr> and <fr>*union*</fr> of two sets are defined as A∩B={*x*\|*x*∈A and *x*∈B} and A∪B={*x*\|*x*∈A or *x*∈B}. The <fr>*difference*</fr> of two sets is defined as A\B={*x*\|*x*∈A and *x*![notin](img/notin.png)B}. It is customary to fix a <fr>*universe of discource*</fr> *U* such that all sets under consideration are subsets of *U*. The <fr>*complemant*</fr> of a set A is defined as ![Abar](img/Abar.png)= U\A. Two sets are <fr>*disjoint*</fr> if their intersection is empty: A∩B=φ. The <fr>*cardinality*</fr> of a set A is its number of elements and is denoted ![al](img/Alength.png). The <fr>*powerset*</fr> of a set A is the set of all its subsets ![2^A](img/2_A.png)={B\|B⊆A}; its cardinality is \|![2^A](img/2_A.png)\|=![20](img/20.png). The <fr>*characteristic function*</fr> of a set A is the function *f*:U&rarr;{*ture*,*false*} such that *f*(*x*)= true if *x*∈A and *f*(*x*)=false if x∈U\A. 

If A and B are sets, the <fr>*Cartesian product*</fr> A x B is the set of all pairs {(*x*,*y*)\|*x*&isin;A and *y*&isin;B}; this generalises to products of more than two sets. A (binary) <fr>*relation*</fr> is a set of pairs R&sube;A x B for some sets A and B; if A=B we say the relation is over A. Instead of (*x*,*y*)&isin;R we also write *x*R*y*. A relation over A is <br>
<fb>*(i)*</fb><fr>*reflexive*</fr> if *x*R*x* for all *x*&isin;A;<br>
<fb>*(ii)*</fb><fr>*symmetric*</fr> if *x*R*y* implies *y*R*x* for all *x*,*y* &isin;A;<br>
<fb>*(iii)*</fb><fr>*antisymmetric*</fr> if *x*R*y* and *y*R*x* implies *x*=*y* for all *x*,*y*&isin;A;<br>
<fb>*(iv)*</fb><fr>*transive*</fr> if *x*R*y* and *y*R*z* implies *x*R*z* for all *x*,*y*,*z*&isin;A.<br>
<fb>*(v)*</fb><fr>*total*</fr> if *x*R*y* or *y*R*x* for all *x*,*y*&isin;A.

A <fr>*partial order*</fr> is a binary relation that is reflexive, antisymmetric and transitive. For instance, the <fr>*subset*</fr> relation &sube; is a partial order. A <fr>*total order*</fr> is a binary relation that is total (hence reflexive), antisymmetric and transive. The &le; relation on real numbers is a total order. If *x*R*y* or *y*R*x* we say that *x* and *y* are <fr>*compatible*</fr>; otherwise they are <fr>*incompatible*</fr>. An <fr>*equivalence relation*</fr> is a binary relation &equiv; that is reflexive, symmetric and transitive. The <fr>*equivalence class*</fr> of *x* is [*x*]={*y*\|*x*&equiv;*y*}. For example, the binary relation 'contains the same number of elements as' over any set is an equivalence relation. Any two equivalence classes are disjoint, and the union of all equivalence classes forms a <fr>*partition*</fr> of the set. If ![21](img/21.png) is a partition of a set A, *i.e.,*![22](img/22.png)=A and ![23](img/23.png) for all i&ne;j, we write ![24](img/24.png). To illustrate this, let *T* be a feature tree, and define a relation ![25](img/25.png) such that *x* ~*T* *x*' if and only if *x* and *x*' are assigned to the same leaf of feature tree *T*, then ~*T* is an equivalence relation, and its equivalence classes are precisely the instance space segmentsassociated with *T*.

---
<fb>**Background 2.1.**</fb> Useful concepts from discrete mathematics.


The sections in this chapter are devoted to the first three scenarios in [Table 2.1](#T2-1): classification in [Section 2.1](#S2-1), scoring and ranking in [Section 2.2](#S2-2) and class probability estimation in [Section 2.3](#S2-3). To keep things manageable we mostly restrict attention to two-class tasks in this chapter and deal with more than two calsses in [Chapter 3](#C3). Regression, unsupervised and descriptive learning will also be considered there.

<a name="T2-1"></a>
<center>![26](img/26.png)<br> <fb>**Table 2.1**</fb> Predictive machine learning scenarios.</center>

Throughout this chapter I will illustrate key conceptsby means of examples using simple models of the kind discussed in the <fb>Prologue</fb>. These models will either be simple tree-based models, representative of grouping models, or linear models, representative of grading models. Sometimes we will even construct models from single features, a setting that could be described as <fr>*univariate machine learning*</fr>. We will start dealing with the question of how to *learn* such models from [Chapter 4](#C4) onwards.

[TOC](#TOC)

## *[2.1 Classification](darkblue|Times)
Classification is the most common task in machine learning. A *[classifier](red) is a mapping \TeX{ \hat{c}:\chi \rightarrow L\TeX}, where \TeX{L = \{C_{1},C_{2},...,C_{k}\}\TeX} is a finite and usually small set of *[class labels](red). We will sometimes also use \TeX{C_{i}\TeX} to indicate the set of examples of that class. We use the 'hat' to indicate that \TeX{ \hat{c}(x) \TeX} is an estimate of the true but unknown function *c(x)*. Examples for a classifier take the form *(x,c(x))*, where \TeX{x \in \chi \TeX} is an instance and *c(x)* is the true class of the instance. Learning a classifier involves constructing the function \TeX{ \hat{c} \TeX} such that it matches *c* as closely as possible (and not just on the training set but ideally on the entire space \TeX{\chi\TeX} ).

In the simplest case we have only two classes which are usually reffered to as *positive* and *negative*, *[&oplus;](green) and *[&ominus;](yellow), or +1 amd -1. Two-class classification is often called *[*binary classification*](red) (or *[*concept learning*](red), if the positive class can be meaningfully called a concept). Spam e-mail filtering is a good example of binary classification, in which spam is conventionally taken as the positive class, and ham as the negative class (clearly, positive here doesn't mean 'good'!). Other examples of binary classification include medical diagnosis (the positive class here is having a particular disease) and credit card fraud detection.

![Fig 2.1.](img/Fig2-1.png)
*[Figure 2.1. (left)](darkblue) A deature tree with training set class distribution in the leaves. *[(right)](darkblue) A decision tree obtained using the majority class decision rule.

The feature tree in *[<u>Figure 2.1(left)</u>](darkblue) can be turned into a classifier by labbeling each leaf with a class. The simplest way to do this is by assigning the *[*majority class*](red) in each leaf, resulting in the decision tree in *[<u>Figure 2.1(right)</u>](darkblue). The classifier works as follows: if an e-mail contains the word 'Viagra' it is classified as spam (right-most leaf); otherwise, the occurrence of the word 'lottery' decides whetherit gets labelled spam or ham. From the numbers in *[<u>Figure 2.1</u>](darkblue) we can get an idea how well this classifier does. The left-most leaf correctly predicts 40 ham e-mails but also mislabels 20 spam e-mails that contain neither 'Viagra' nor 'lottery'. The middle leaf correctly classifiers 10 spam e-mails but also erroneously labels 5 ham e-mails as spam. The 'Viagra' test correctly picks out 20 spam e-mails but also 5 ham e-mails. Taken together, this means that 30 out of 50 spam e-mails are classified correctly, and 40 out of 50 ham e-mails.

### *[Assesing classification performance](darkblue)
The pergormance of such classifiers can be summarized by means of a table known as a *[*contingency table*](red) or *[*confusion matrix*](red) ([Table 2.2(left)](#Table2-2)). In this table, each row refers to actual classes as recorded in the test set, and eacj column to classes as predicted by classifier. So, for instance, the first row states that the test set contains 50 positives, 30 of which were correctly predicted and 20 incorrectly. The last column and the last row give the *[*marginals*](red) (i.e., column and row sums). Marginals are important because they allow us to asses statistical significance. For instance, the contigency table in [Table 2.2(right)](#Table2-1) has the same marginals, but the classifier clearly makes a random choice as to which predictions are positive and which are negative -- as a result the distribution of actual positives and negatives in either predicted class is the same as the overall distribution (uniform in this case).


[*[Table 2.2.(left)](darkblue) A two-class contingency table or confusion matrix depicting the performance of the decision tree in *[<u>Figure 2.1</u>](darkblue). Numbers on the descending diagonal indicate correct predictions, while the ascending diagonal concerns prediction errors. *[(right)](darkblue) A contingency table with the same marginals but independed rows and columns.]{#Table2-2}
|                   |Predicted *[(+)](green)    |Predicted *[(-)](yellow)     |  |             |    |*[(+)](green)|*[(-)](yellow)|      ||
|------------------:|---------------------------|-----------------------------|--|-------------|-----------------|----------|--|-|
|Actual*[(+)](green)|*[30](darkblue) |*[20](red)|50                           |  |*[(+)](green)|*[20](darkblue)  |*[30](red)|50||
|Actual*[(-)](yellow)|*[10](red)     |*[40](darkblue)|50                      |  |*[(-)](yellow)|*[20](red)|*[30](darkblue)|50||
|                    |40             |60        |100                          |  |              |40        |60             |100||



From a contingency table we can calculate a range of performance indicators. The simplest of these is *[*accuracy*](red), which is the proportion of correctly classified test instances. In the notation introduced at the beginning of this chapter, accuracy over a test set *Te*is defined as 

\TeX{acc = \frac{1}{|T_{e}|}\sum^{}_{x \in T_{e}}I[\hat{c}(x) = c(x)]  ...  (2.1)\TeX}


Here, the function I[...] denotes the *[*indicator function*](red), which is 1 if its argument evaluates to true, and o otherwise. In this case it is a convenient way to count the number of test instances that are classified correctly by the classifier (i.e., the estimated class label \TeX{\hat{c}(x)\TeX} is equal to the true class label *c(*x*)*). For example, in [Table 2.2(left)](#Table2-2) the accuracy of the classifier is 0.70 or 70%, and in [Table 2.2(right)](#Table2-2) it is 0.50. Alternatively, we can calculate the *[*error rate*](red) as the proportion of incorrectly classified instances, here 0.30 and 0.50, respectively. Clearly, accuracy and error rate sum to 1.

Test set accuracy can be seen as an *estimate* of the probability that an arbitrary instance \TeX{x \in \chi \TeX} is classified correctly: more precisely, it estimates the probability

\TeX{P_{\chi}(\hat{c}(x) = c(x)) \TeX}

(Notice that Iwrite \TeX{P_{\chi}\TeX} to emphasise that this is a probability distribution over the instance space &chi;; I will often omit subscripts if this is clear from the context.) We typically only have access to the true classes of a small fraction of the instance space and so an estimate is all we can hope to get. It is therefore important that the test set is as representative as possible. This is usually formalised by the assumption that the occurrence of instances in the world -- i.e., how likely or typical a particular e-mail is -- is governed by an unknown probability distribution on &chi;, and that the test set \TeX{T_{e}\TeX} is generated according to this distribution.

It is often convenient -- not to say neccesary -- to distinguish performance on the classes. To this end, we need some further terminology. Correctly classified positives and negatives are referred to as *[*true positives*](red) and *[*true negatives*](red), respectively. Incorrectly classified positives are perhaps are, perhaps somewhat confusingly, called *[*false negatives*](red); similarly, misclassified negatives are called *[*false positives*](red). A good way to think of this is to remember that positive/negative refers to the classifier's prediction, and true/false refers to whether the prediction is correct or not. So, a false positive is something that was incorrectly predicted as positive, and therefore an actual negative (e.g., a ham e-mail misclassified as spam, or a healthy patient misclassified as having the disease in question). In the previous example ([Table 2.2(left)](#Table2-2)) we have 30 true positives, 20 false negatives, 40 true negatives and 10 false positives.

The *[*true positive rate*](red) is the proportion of positives correctly classified, and can be defined mathematically as
\TeX{tpr = \frac{\sum_{x \in T_{e}}I[\hat{c}(x)=c(x)=\oplus]}{\sum_{x \in T_{e}}I[c(x)=\oplus]} ... (2.2) \TeX}
True positive rate is an estimate of the probability that an arbitrary positive is classified correctly, that is, an estimate of \TeX{P_{\chi}(\hat{c}(x)=\oplus\|c(x)=\oplus)\TeX}. Analogously, the *[*true negative rate*](red) is the proportion of negatives correctly classified (see [Table 2.3](#Table2-3) for the mathematical definition), and estimates \TeX{\P_{\chi}(\hat{c}(x)=\ominus|c(x)=\ominus)\TeX}. These rates, which are sometimes called *[*sensitivity*](red) and *[*specificity*](red), can be seen as per-class accuracies. In the contingency table, the true positive and negative rates can be calculated by dividing the number on the descending (good) diagonal by the row total. We can also talk about per-class error rates, which is the *[*false negative rate*](red) for the positives (i.e., the number of misclassified positives or false negatives as a proportion of the total number of positives) and the *[*false positive rate*](red) for the negatives (sometimes called the *[*false alarm rate*](red)). THese rates can be found by dividing the number on the ascending (bad) diagonal by the row total.

In [Table 2.2 (left)](#Table2-2) we have a true positive rate 60%, a true negative rate of 80%, a false negative rate of 40% and a false positive rate of 20%. In [Table 2.2 (right)](#Table2-2) we have a true positive rate of 40%, a true negative rate of 60%, a false negative rate of 60% and a false positive rate of 40%. Notice that the accuracy in both cases is the average of the true positive rate and the true negative rate (and the error rate is the average of the false positive rate and the false negative rate). However, this is true only if the test set contains equal numbers of positives and negatives--in the generral case we need to use a *weighted* average, where the weights are the propotions of positives and negatives in the test set.

|-:b=2 solid black w=600 h=auto rad=5----------------------------------------------------------------------------------------|

abcdef

|-:b=2 solid black w=450 h=auto rad=5----------------------------------------------------------------------------------------|

<hr>                                                                                                                        
### *[Example 2.1 (Accuracy as a weighted average).](darkblue)                                                              
Suppose a classifier's predictions on a test set are as in the following table:                                             
                                                                                                                            
|                    |Predicted *[(+)](green)|Predicted *[(-)](yellow)|   |
|:-------------------|:----------------------|:-----------------------|:--|
|Actual*[(+)](green) |*[60](darkblue)        |*[15](red)              |75 |
|Actual*[(-)](yellow)|*[10](red)             |*[15](darkblue)         |25 |
|                    |70                     |30                      |100|

From this table, we see that the true positive rate is *tpr* = 60/75 = 0.80 and the true negative rate is *tnr* = 15/25 =   
0.60. The overall accuracy is *acc* = (60 + 15)/100 = 0.75, which is no longer the average of true positive and negative    
rates. However, taking into account the proportion of positives *pos* = 0.75 and the proportion of negatives *neg* = 1 - pos
 = 0.25, we see that<br />
                                \TeX{acc = pos \cdot tpr + neg \cdot tnr ...(2.3)\TeX}<br />
This equation holds in general: if the numbers of positives and negatives are equal, we obtain the unweighted average from 
the earlier example(*acc*=(*tpr* + *tnr*)/2).  

|____________________________________________________________________________________________________________________________|

bbbbbb

<br />
|-:b=2 solid black w=400 h=auto rad=5----------------------------------------------------------------------------------------|

aaaaaaa

|_________________________________|


hijklm

|____________________________________________________________________________________________________________________________|

opqrstu


|                    |Predicted *[(+)](green)|Predicted *[(-)](yellow)|   |
|--------------------|-----------------------|------------------------|---|
|Actual*[(+)](green) |*[60](darkblue)        |*[15](red)              |75 |
|Actual*[(-)](yellow)|*[10](red)             |*[15](darkblue)         |25 |
|                    |70                     |30                      |100|


[TOC](#TOC)
