package cn.edu.ldu.flower.network
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
private const val BASE_URL = "http://159.250.250.000/"  //Web服务器地址
// 定义 Retrofit 框架，数据对象采用 moshi
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()
// 定义网络服务接口，声明与服务器通信的方法
interface ApiService {
    @POST("predict_flower")  // 此处注解修饰 Web API服务的名称
    fun getPredictResult(@Body body: RequestBody) : Call<ResponseBody>
}
object ResultApi {     // 创建全局对象，便于网络访问
    val retrofitService : ApiService by lazy {
        retrofit.create(ApiService::class.java) }
}
