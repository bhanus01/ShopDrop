package com.example.shopdrop.data.firestore

import android.net.Uri
import com.example.shopdrop.common.Constants
import com.example.shopdrop.common.Resource
import com.example.shopdrop.data.model.UserAddressDto
import com.example.shopdrop.data.model.UserDto
import com.example.shopdrop.data.model.UserOrderDto
import com.example.shopdrop.domain.model.Cart
import com.example.shopdrop.domain.model.filterList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FireStoreOperations @Inject constructor(
    private val fireStore: FirebaseFirestore,
    private val firebaseStorage: StorageReference,
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun updateWishlist(userId: String, productId: String): Resource<Boolean> {
        try {
            val userDto = fireStore.collection("users").document(userId).get().await()
            val map = mutableMapOf<String, Any>()
            if (userDto.exists()) {
                val userWishlist: MutableList<String> =
                    userDto.toObject(UserDto::class.java)!!.userWishlist as MutableList<String>
                if (userWishlist.contains(productId)) {
                    userWishlist.remove(productId)
                } else {
                    userWishlist.add(productId)
                }

                map["userWishlist"] = userWishlist

                fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()
            }
            return Resource.Success(true)
        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }

    suspend fun updateCart(
        userId: String,
        productId: String,
        selectedSize: String,
        action: String
    ): Resource<Boolean> {
        try {
            val userDto = fireStore.collection("users").document(userId).get().await()

            val map = mutableMapOf<String, Any>()
            if (userDto.exists()) {

                val userCart = userDto.toObject(UserDto::class.java)!!.userCart
                val productList =
                    userCart.filter { it.filterList(productId, selectedSize) }

                if (productList.isNotEmpty()) {
                    val index = userCart.indexOf(productList[0])
                    when (action) {

                        Constants.INCREASE -> {
                            productList[0].quantity += 1
                            userCart.removeAt(index)
                            userCart.add(index, productList[0])
                        }

                        Constants.DECREASE -> {
                            if (productList[0].quantity == 1) {
                                userCart.removeAt(index)
                            } else {
                                productList[0].quantity -= 1
                                userCart.removeAt(index)
                                userCart.add(index, productList[0])
                            }
                        }

                        Constants.REMOVE -> {
                            userCart.removeAt(index)
                        }

                    }

                } else {
                    userCart.add(Cart(productId, 1, selectedSize))
                }

                map["userCart"] = userCart

                fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()
            }
            return Resource.Success(true)
        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }

    suspend fun updateProfile(
        userId: String,
        name: String?,
        email: String?,
        phone: Long?,
        uri: Uri?
    ): Resource<Boolean> {
        try {
            val userDto = fireStore.collection("users").document(userId).get().await()
            val map = mutableMapOf<String, Any>()
            if (userDto.exists()) {
                val userProfile = userDto.toObject(UserDto::class.java)!!.userProfile

                if (name != null) {
                    userProfile.userName = name
                }
                if (email != null) {
                    userProfile.userEmail = email
                }
                if (phone != null) {
                    userProfile.userPhone = phone
                }

                map["userProfile"] = userProfile

                fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()

                if (uri != null) {
                    firebaseStorage.child("/user-images/${firebaseAuth.currentUser!!.uid}/profile_image.jpg")
                        .putFile(uri).await()
                }

            }
            return Resource.Success(true)
        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }


    suspend fun updateAddress(
        action: String,
        userId: String,
        index: Int,
        address: UserAddressDto
    ): Resource<Boolean> {
        try {
            val userDto = fireStore.collection("users").document(userId).get().await()
            val userAddress =
                userDto.toObject(UserDto::class.java)!!.userAddress as MutableList<UserAddressDto>
            val map = mutableMapOf<String, Any>()

            if (action == Constants.UPDATE) {
                if (index == -1) {
                    if (userAddress.size == 0) {
                        address.defaultAddress = true
                        userAddress.add(address)
                    } else {
                        if (address.defaultAddress) {
                            for (add in userAddress) {
                                add.defaultAddress = false
                            }
                            userAddress.add(address)
                        } else {
                            userAddress.add(address)
                        }
                    }
                } else {
                    if (address.defaultAddress) {
                        for (add in userAddress) {
                            add.defaultAddress = false
                        }
                        userAddress[index] = address
                    } else {
                        userAddress[index] = address
                    }

                }
            } else if (action == Constants.REMOVE) {
                val removedAddress = userAddress[index]
                if (removedAddress.defaultAddress) {
                    if (userAddress.size == 1) {
                        userAddress.removeAt(index)
                    } else {
                        userAddress[0].defaultAddress = true
                        userAddress.removeAt(index)
                    }
                } else {
                    userAddress.removeAt(index)
                }
            }

            map["userAddress"] = userAddress

            fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()

            return Resource.Success(true)

        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }

    suspend fun emptyCart(userId: String): Resource<Boolean> {
        return try {
            val map = mutableMapOf<String, Any>()
            val cartList = mutableListOf<Cart>()
            map["userCart"] = cartList
            fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message.toString())
        }
    }


    suspend fun addOrder(userId: String, orderDto: List<UserOrderDto>): Resource<Boolean> {
        try {
            val userDto = fireStore.collection("users").document(userId).get().await()
            val userOrders = userDto.toObject(UserDto::class.java)!!.userOrders
            val map = mutableMapOf<String, Any>()
            if (userDto.exists()) {
                userOrders.addAll(orderDto)
            }

            map["userOrders"] = userOrders

            fireStore.collection("users").document(userId).set(map, SetOptions.merge()).await()

            return Resource.Success(true)

        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }

}