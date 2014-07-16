import glob
import csv

def printL(listMessages):
	for item in listMessages:
		print(item[0])
	return 

def search(val,listMessages):
	i=0
	#print("searching"+str(len(listMessages)))
	for item in listMessages:
		if(item[0]==val):
			return i
		i=i+1
	return -1	

def fillListFile1(file1):
	#read file1 into list
	ifile = open(file1, "rU")
	reader = csv.reader(ifile)
	rowdata = []
        for row in reader:
            rowdata.append(row)
	#printL(rowdata)
	return rowdata
	

def compare(file1,file2):
	rowdata =fillListFile1(file1)
	#open file 2
	ifile = open(file2, "rU")
	reader2 = csv.reader(ifile)
	searchIndex=0
	oldVal=""
	for row in reader2:
		#print("Searching "+row[0])
		temp=search(row[0],rowdata)
		#print("Found "+str(temp))
		if(temp==-1):
			#print("continue")
			continue
		elif(temp>=searchIndex):
			#print("temp>")
			searchIndex=temp
		else:
			print("\n \tFailed For Message ID -"+row[0])
			print("\tIn File "+file1)
			print("\tCurrent Value "+row[0]+" is at "+str(temp))
			print("\tPrevValue "+oldVal+" is at "+str(searchIndex))
			return 1
		oldVal=row[0]
	return 0

def main():
	
	if(len(listFileNames)==0):
		print("can't read files")
		return
	for x in range(0, len(listFileNames)):
		if(x!=len(listFileNames)-1):
			y=x+1
			while(y!=len(listFileNames)):
				print("Comapring "+listFileNames[x]+" with "+listFileNames[y])
				if(compare(listFileNames[x],listFileNames[y])):
					print("OOps!!Above Files are Not In Total order")
					return
				y=y+1
				print("Above Files are in Total Order")
	print("In Totally orderA")
	return
#global
index=0
listMessages=[]
listFileNames=glob.glob("log/*.log")
main()


