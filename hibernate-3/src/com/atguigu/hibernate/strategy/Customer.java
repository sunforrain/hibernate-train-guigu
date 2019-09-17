package com.atguigu.hibernate.strategy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Customer {

	private Integer customerId;
	private String customerName;

	private Set<Order> orders = new HashSet<>();

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public Set<Order> getOrders() {
		return orders;
	}

	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(customerId);
    }
}
