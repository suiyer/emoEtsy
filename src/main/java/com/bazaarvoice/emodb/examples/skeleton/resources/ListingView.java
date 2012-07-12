package com.bazaarvoice.emodb.examples.skeleton.resources;

import com.yammer.dropwizard.views.View;

import java.util.Map;

public class ListingView extends View {

    private final Map<String, Object> listing;

    public ListingView(Map<String, Object> listing) {
        super("listing.ftl");
        this.listing=listing;
    }

    public Map<String, Object> getListing() {
        return listing;
    }
}
