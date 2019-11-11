package com.squareup.subzero.server;

import com.squareup.subzero.server.resources.AssetsResource;
import com.squareup.subzero.server.resources.ComputeResource;
import com.squareup.subzero.server.resources.ConstantsResource;
import com.squareup.subzero.server.resources.GenerateQrCodeResource;
import com.squareup.subzero.server.resources.PrettyPrintResource;
import com.squareup.subzero.server.resources.ShowFinalTransactionResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ServerApplication extends Application<ServerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new ServerApplication().run(args);
    }

    @Override
    public String getName() {
        return "Subzero server";
    }

    @Override
    public void initialize(final Bootstrap<ServerConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/assets/"));
    }

    @Override
    public void run(final ServerConfiguration configuration, final Environment environment) {
        JerseyEnvironment jersey = environment.jersey();
        jersey.register(new AssetsResource());
        jersey.register(new ConstantsResource());
        jersey.register(new PrettyPrintResource());
        jersey.register(new GenerateQrCodeResource());
        jersey.register(new ComputeResource());
        jersey.register(new ShowFinalTransactionResource());
        jersey.register(new ServerExceptionMapper());
    }
}
