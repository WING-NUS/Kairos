TARGET=CRFPP
JAVAC=javac -encoding UTF-8
JAVA=java
JAR=jar
CXX=c++
INCLUDE=/usr/local/jdk/include
INCLUDEOSX=/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Headers

PACKAGE=org/chasen/crfpp

LIBS=-lcrfpp -lpthread -L../lib/
INC=-I$(INCLUDE) -I$(INCLUDE)/linux -I$(INCLUDEOSX) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I../include

all:
	$(CXX) -O3 -c -fpic $(TARGET)_wrap.cxx $(INC)
	$(CXX) -shared  $(TARGET)_wrap.o -o lib$(TARGET).so $(LIBS)
	$(JAVAC) $(PACKAGE)/*.java
	$(JAVAC) test.java
	$(JAR) cfv $(TARGET).jar $(PACKAGE)/*.class

test:
	cp ../../CrfppPaperMetadataModel ./
	$(JAVA) test
	rm CrfppPaperMetadataModel

clean:
	#rm -fr *.jar *.o *.so *.class $(PACKAGE)/*.class
	rm -fr *.jar *.o *.class $(PACKAGE)/*.class

cleanall:
	rm -fr $(TARGET).java *.cxx
