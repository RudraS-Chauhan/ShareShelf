package com.shareshelf.model;

public class ResourceItem {

    private int itemId;
    private String ownerId, title, description, category, listingType, itemCondition, location, status;

    public ResourceItem() {}

    public int getItemId()
    {
        return itemId;
    }
    public void setItemId(int itemId)
    {
        this.itemId = itemId;
    }

    public String getOwnerId()
    {
        return ownerId;
    }
    public void setOwnerId(String ownerId)
    {
        this.ownerId = ownerId;
    }
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getCategory()
    {
        return category;
    }
    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getListingType()
    {
        return listingType;
    }
    public void setListingType(String listingType)
    {
        this.listingType = listingType;
    }   

    public String getItemCondition()
    {
        return itemCondition;
    }
    public void setItemCondition(String itemCondition)
    {
        this.itemCondition = itemCondition;
    }
    public String getLocation()
    {
        return location;
    }
    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getStatus()
    {
        return status;
    }
    public void setStatus(String status)
    {
        this.status = status;
    }
}