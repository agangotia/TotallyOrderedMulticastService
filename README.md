TotallyOrderedMulticastService
==============================

Implementation of a totally ordered multicast service USING Skeen's Algorithm.
#skeen, #totallyorderedmulticastservice, #multicastimplementation, #multicast

Platform : UBUNTU

How to Run
------------------------------------------------
TO Build :
1.cd to location of TotallyOrderedMulticastService
2.ant

To run:
./runScript.sh
this runs the 10 processes
(chmod u+x runScript.h)// need to assign execute permission to script

/data
This folder contains the Topology information and messages information.
topology.txt : In the form of PID,IPADDRESS,PORTNO
multicast.txt : Multicast Message In the form of SENDERNODEID,RECIEVERIP,RECIEVERPORT,MESSAGE.


scriptTest.py : The Testing Python script.
Before you execute ./runScript.sh make sure you clear log folder.
python scriptTest.py

Reference Documents : folder contains refrence for logical clock and skeen's algorithm.

Implementation Details
----------------------
Need to add

