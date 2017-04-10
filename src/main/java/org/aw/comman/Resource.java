package org.aw.comman;

import org.aw.server.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YURI-AK on 2017/4/5.
 */
public class Resource implements Cloneable {
    private String name;
    private String description;
    private List<String> tags;
    private URI uri;
    private long size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    private String channel;
    private String owner;
    private Server server;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static boolean checkValidity(JSONObject resourceObject) {
        return (resourceObject.has("name") && resourceObject.has("tags") && resourceObject.has("description") && resourceObject.has("uri") && resourceObject.has("channel") && resourceObject.has("owner") && resourceObject.has("ezserver"));
    }

    public static JSONObject toJson(Resource resource){
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("name",resource.getName()==null?"":resource.getName());
        JSONArray tagArray=new JSONArray();
        if (resource.getTags()!=null){
            for (String tag : resource.getTags()) {
                tagArray.put(tag);
            }
        }
        jsonObject.put("tags",tagArray);
        jsonObject.put("description",resource.getDescription()==null?"":resource.getDescription());
        jsonObject.put("uri",resource.getUri()==null?"":resource.getUri().toString());
        jsonObject.put("channel",resource.getChannel()==null?"":resource.getChannel());
        jsonObject.put("owner",resource.getOwner()==null?"":resource.getOwner());
        jsonObject.put("ezserver",resource.getServer()==null?"":resource.getServer().toString());
        if (resource.getSize()>0){
            jsonObject.put("resourceSize",resource.getSize());
        }
        return jsonObject;
    }

    public static Resource parseJson(JSONObject resourceObject){
        if(!checkValidity(resourceObject)){
            return null;
        }
        String name=resourceObject.getString("name");
        String description=resourceObject.getString("description");
        String uriString=resourceObject.getString("uri");
        URI uri = null;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String owner=resourceObject.getString("owner");
        String channel=resourceObject.getString("channel");
        String ezServerString=resourceObject.getString("ezserver");
        Server server=null;
        if (!ezServerString.equals("")){
            String ezHost = ezServerString.split(":")[0];
            int port = Integer.parseInt(ezServerString.split(":")[1]);
            server = new Server(ezHost, port);
        }
        JSONArray tagArray=resourceObject.getJSONArray("tags");
        List<String> tagList=new ArrayList<>();
        for (int i = 0; i < tagArray.length(); i++) {
            tagList.add(tagArray.getString(i));
        }
        Resource resource=new Resource();
        resource.setName(name);
        resource.setChannel(channel);
        resource.setDescription(description);
        resource.setOwner(owner);
        resource.setUri(uri);
        resource.setTags(tagList);
        resource.setServer(server);
        if (resourceObject.has("resourceSize")){
            resource.setSize(resourceObject.getInt("resourceSize"));
        }
        return resource;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Resource)) return false;
        Resource resource=(Resource) obj;
        return resource.getOwner().equals(this.owner)&&resource.getChannel().equals(this.channel)&&resource.getUri().equals(this.uri);
    }

    @Override
    public Resource clone() throws CloneNotSupportedException {
        Resource copiedResource=new Resource();
        copiedResource.setName(new String(this.getName()));
        copiedResource.setDescription(new String(this.getDescription()));
        copiedResource.setChannel(new String(this.getChannel()));
        copiedResource.setOwner(new String(this.getOwner()));
        try {
            copiedResource.setUri(new URI(this.getUri().toString()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (this.getServer()!=null){
            copiedResource.setServer(new Server(this.getServer().getHostname(), this.getServer().getPort()));
        }
        List<String> copiedTags = new ArrayList<>();
        if (this.getTags()!=null){
            List<String> tags = this.getTags();
            tags.forEach(tag -> copiedTags.add(tag));
        }
        copiedResource.setTags(copiedTags);
        copiedResource.setSize(this.size);
        return copiedResource;
    }
}
