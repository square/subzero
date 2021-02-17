# USAGE:
#   docker build -t subzero .
#   docker run -t -p 32366:32366 --name subzero subzero

FROM centos:8.1.1911

RUN yum -y --enablerepo=extras install \
    autoconf \
    automake \
    clang \
    cmake \
    dnf-plugins-core \
    gcc \
    gcc-c++ \
    git \
    libtool \
    make \
    python2 \
    unzip \
    vim

RUN yum config-manager --set-enabled PowerTools && \
    yum install -y protobuf-devel && \
    ln -s /usr/bin/python2 /usr/bin/python && \
    pip2 install protobuf

ADD CMakeLists.txt /subzero/CMakeLists.txt
ADD include /subzero/include
ADD nanopb /subzero/nanopb
ADD proto /subzero/proto
ADD src /subzero/src
ADD trezor-crypto /subzero/trezor-crypto
ADD external-crypto /subzero/external-crypto

WORKDIR /subzero

RUN mkdir /build && cd /build && TARGET=dev CURRENCY=btc-testnet cmake /subzero && make

EXPOSE 32366

CMD ["/build/subzero"]
