WatchDog : tools.o watch.o
	g++ -I/usr/local/include/opencv2 \
            -lopencv_core \
            -lopencv_imgcodecs \
            -lopencv_highgui \
            -lopencv_imgproc \
            -lopencv_videoio \
            -o WatchDog tools.o watch.o

tools.o : tools.cpp tools.h
	g++ -c tools.cpp

watch.o : watch.cpp
	g++ -c watch.cpp
