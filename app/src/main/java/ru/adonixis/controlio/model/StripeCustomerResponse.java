package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class StripeCustomerResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("account_balance")
    private Integer accountBalance;

    @SerializedName("created")
    private Long created;

    @SerializedName("currency")
    private String currency;

    @SerializedName("default_source")
    private String defaultSource;

    @SerializedName("delinquent")
    private Boolean delinquent;

    @SerializedName("description")
    private String description;

    @SerializedName("discount")
    private Object discount;

    @SerializedName("email")
    private String email;

    @SerializedName("livemode")
    private Boolean livemode;

    @SerializedName("metadata")
    private Object metadata;

    @SerializedName("shipping")
    private Object shipping;

    @SerializedName("sources")
    private Sources sources;

    @SerializedName("subscriptions")
    private Subscriptions subscriptions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Integer getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(Integer accountBalance) {
        this.accountBalance = accountBalance;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public Boolean isDelinquent() {
        return delinquent;
    }

    public void setDelinquent(Boolean delinquent) {
        this.delinquent = delinquent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDiscount() {
        return discount;
    }

    public void setDiscount(Object discount) {
        this.discount = discount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean isLivemode() {
        return livemode;
    }

    public void setLivemode(Boolean livemode) {
        this.livemode = livemode;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public Object getShipping() {
        return shipping;
    }

    public void setShipping(Object shipping) {
        this.shipping = shipping;
    }

    public Sources getSources() {
        return sources;
    }

    public void setSources(Sources sources) {
        this.sources = sources;
    }

    public Subscriptions getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Subscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    public class Sources {
        @SerializedName("object")
        private String object;

        @SerializedName("data")
        private List<StripeSourceResponse> data = new ArrayList<>();

        @SerializedName("hasMore")
        private Boolean hasMore;

        @SerializedName("totalCount")
        private Integer totalCount;

        @SerializedName("url")
        private String url;

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public List<StripeSourceResponse> getData() {
            return data;
        }

        public void setData(List<StripeSourceResponse> data) {
            this.data = data;
        }

        public Boolean hasMore() {
            return hasMore;
        }

        public void setHasMore(Boolean hasMore) {
            this.hasMore = hasMore;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public class Subscriptions {
        @SerializedName("object")
        private String object;

        @SerializedName("data")
        private List<Object> data = new ArrayList<>();

        @SerializedName("has_more")
        private Boolean hasMore;

        @SerializedName("total_count")
        private Integer totalCount;

        @SerializedName("url")
        private String url;

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public List<Object> getData() {
            return data;
        }

        public void setData(List<Object> data) {
            this.data = data;
        }

        public Boolean hasMore() {
            return hasMore;
        }

        public void setHasMore(Boolean hasMore) {
            this.hasMore = hasMore;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}