FROM centos:7

RUN yum -y install \
    autoconf \
    automake \
    clang \
    cmake \
    gcc \
    gcc-c++ \
    git \
    libtool \
    make \
    python-setuptools \
    unzip \
    vim

# Protobuf support: 3.5.2
RUN mkdir protobuf && \
    cd protobuf && \
    curl -sSOL https://github.com/google/protobuf/archive/v3.5.2.zip && \
    unzip v3.5.2.zip && \
    cd protobuf-3.5.2 && \
    ./autogen.sh && \
    ./configure && \
    make install && \
    ldconfig && \
    cd python && python setup.py install

# Add nCipher tools, which unpack to /opt/nfast
WORKDIR /
RUN curl -sSL https://<insert your local url to ncipher-tools>/gccsrc-ppcdev_12.20.51.tar.gz | \
    tar -xz && \
    curl -sSL https://<insert your local url to ncipher-tools>/csd-agg_12.20.51.tar.gz | \
    tar -xz

ADD . /subzero

WORKDIR /subzero

RUN mkdir /build && cd /build && CURRENCY=btc-testnet cmake /subzero && make

RUN mkdir /crossbuild && cd /crossbuild && TARGET=nCipher CURRENCY=btc-testnet cmake /subzero && make

EXPOSE 32366

CMD ["/build/subzero"]
