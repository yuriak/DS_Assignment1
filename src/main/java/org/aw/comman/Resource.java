package org.aw.comman;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

    public ServerBean getServerBean() {
        return serverBean;
    }

    public void setServerBean(ServerBean serverBean) {
        this.serverBean = serverBean;
    }

    private String channel;
    private String owner;
    private ServerBean serverBean;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static boolean checkValidity(JSONObject resourceObject) {
        return (resourceObject.containsKey("name") && resourceObject.containsKey("tags") && resourceObject.containsKey("description") && resourceObject.containsKey("uri") && resourceObject.containsKey("channel") && resourceObject.containsKey("owner") && resourceObject.containsKey("ezserver"));
    }

    public static JSONObject toJson(Resource resource){
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("name",resource.getName()==null?"":resource.getName());
        JSONArray tagArray=new JSONArray();
        if (resource.getTags()!=null){
            for (String tag : resource.getTags()) {
                tagArray.add(tag);
            }
        }
        jsonObject.put("tags",tagArray);
        jsonObject.put("description",resource.getDescription()==null?"":resource.getDescription());
        jsonObject.put("uri",resource.getUri()==null?"":resource.getUri().toString());
        jsonObject.put("channel",resource.getChannel()==null?"":resource.getChannel());
        jsonObject.put("owner",resource.getOwner()==null?"":resource.getOwner());
        jsonObject.put("ezserver",resource.getServerBean()==null?"":resource.getServerBean().toString());
        if (resource.getSize()>0){
            jsonObject.put("resourceSize",resource.getSize());
        }
        return jsonObject;
    }

    public static Resource parseJson(JSONObject resourceObject){
        if(!checkValidity(resourceObject)){
            return null;
        }
        String name= (String) resourceObject.get("name");
        String description= (String) resourceObject.get("description");
        String uriString= (String) resourceObject.get("uri");
        URI uri = null;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String owner= (String) resourceObject.get("owner");
        String channel= (String) resourceObject.get("channel");
        String ezServerString= (String) resourceObject.get("ezserver");
        ServerBean serverBean =null;
        if (!ezServerString.equals("")){
            String ezHost = ezServerString.split(":")[0];
            int port=0;
            try {
                port = Integer.parseInt(ezServerString.split(":")[1]);
                if(port<0||port>65535){
                    return null;
                }
            }catch (Exception e){
                return null;
            }
            serverBean = new ServerBean(ezHost, port);
        }
        JSONArray tagArray= (JSONArray) resourceObject.get("tags");
        List<String> tagList=new ArrayList<>();
        for (int i = 0; i < tagArray.size(); i++) {
            tagList.add((String) tagArray.get(i));
        }
        Resource resource=new Resource();
        resource.setName(name);
        resource.setChannel(channel);
        resource.setDescription(description);
        resource.setOwner(owner);
        resource.setUri(uri);
        resource.setTags(tagList);
        resource.setServerBean(serverBean);
        if (resourceObject.containsKey("resourceSize")){
            resource.setSize((Long) resourceObject.get("resourceSize"));
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
        if (this.getServerBean()!=null){
            copiedResource.setServerBean(new ServerBean(this.getServerBean().getHostname(), this.getServerBean().getPort()));
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
