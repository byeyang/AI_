package cn.edu.ldu.flower
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.edu.ldu.flower.databinding.ActivityMainBinding
import cn.edu.ldu.flower.network.Result
import cn.edu.ldu.flower.network.ResultApi
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding //视图绑定类
    //常量定义
    companion object {
        private const val TAG = "FlowerRecognition"
        private const val REQUEST_CODE_PERMISSIONS = 10  //程序中标识权限的常量
        private const val REQUEST_CODE_CAMERA = 20  //标识相机权限的常量
        private const val REQUEST_CODE_GALLERY = 30 //标识相册权限的常量
        //需要申请的权限列表
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    // MainActivity初始化
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Activity中显示主界面视图
        binding.imageView.setImageResource(R.drawable.flower1)  //显示到界面的imageView___________________
        // 拍照按钮的事件侦听，开启相机，拍摄照片，发送照片
        binding.btnCapture.setOnClickListener {
            // 判断是否拥有相机使用权
            if (allPermissionsGranted()) {   //已经授权
                //打开相机，相机拍摄的图片返回给回调函数处理
                var cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA)
            } else {   //否则，表明无权限，需要询问用户，是否授予授权
                ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS,   // 相机和相册权限列表
                    REQUEST_CODE_PERMISSIONS)
            }
        }
        //图库（相册）按钮的事件侦听，打开相册，选择图片，发送图片
        binding.btnLoadPicture.setOnClickListener {
            // 判断是否拥有相册访问权
            if (allPermissionsGranted()) {   //已经授权
                //打开相册，选择的图片返回给回调函数处理
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_CODE_GALLERY)
            } else {   //否则，表明无权限，需要询问用户，是否授予授权
                ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS)
            }
        }
    }
    //判断是否已经开启所需的全部权限（相机和相册）
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    //回调函数，相机拍照之后或者从相册选择图片之后，自动调用本函数
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAMERA) {  //这是来自相机拍照之后的回调
            var bitmap: Bitmap? = data?.getParcelableExtra("data") //相机拍摄的图片
            if (bitmap != null) {
                binding.txtResult.text = "recognising..."
                binding.txtmessage.visibility = View.INVISIBLE
                binding.txtmessage.setText(""); // 设置文本内容
                recognition(bitmap)  //识别图片，调用识别模块
            }
        }
        //这是来自从相册选择图片之后的回调
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_GALLERY) {
            val selectedPhotoUri = data?.data   //返回图片的 Uri
            val bitmap: Bitmap
            try {
                selectedPhotoUri?.let {
                    if (Build.VERSION.SDK_INT < 28) {
                        bitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver, selectedPhotoUri)  //获取图片
                    } else {
                        val source = ImageDecoder.createSource(   //获取图片
                            this.contentResolver,
                            selectedPhotoUri
                        )
                        bitmap = ImageDecoder.decodeBitmap(source)  //解码图片
                    }
                    binding.txtResult.text = "recognising..."
                    binding.txtmessage.visibility = View.INVISIBLE
                    binding.txtmessage.setText(""); // 设置文本内容
                    recognition(bitmap) //识别图片，调用识别模块
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    //识别图片，内部封装了与服务器互动的逻辑，因为识别逻辑实在服务器端完成的
    private fun recognition(bitmap: Bitmap) {
        binding.imageView.setImageBitmap(bitmap)  //显示到界面的imageView上
        //图像转为Base64编码
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        val convertImage: String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        //定义Json对象，因为服务器端接收Json对象
        val imageObject = JSONObject()
        imageObject.put(
            "image",
            convertImage
        )//Base64 image
        //封装到 Retrofit 的 RequestBody 对象中
        val body: RequestBody =
            RequestBody.create(
                MediaType.parse("application/json"),
                imageObject.toString()
            )
        //重写 Retorfit 服务中定义的接口方法
        ResultApi.retrofitService.getPredictResult(body).enqueue(object :
            Callback<ResponseBody>  {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>)
            {
                if (response.isSuccessful) {
                    val result = response.body()?.string()
                    if (result != null) {
                        // 显示预测结果
                        var gson = Gson()
                        var result2 = gson.fromJson(
                            result,
                            Result::class.java
                        )
//                        println(result.toString())
//                        binding.txtmessage.setText(result.string())
                        binding.txtResult.text = result2.prediction
                    } else {
                        // 处理结果为null的情况
                        Log.e(TAG, "Response body is null")
                        binding.txtResult.text = "fail to recognise!!!"
                    }
                } else {
                    // 处理响应失败的情况
                    Log.e(TAG, "Request failed with code: ${response.code()}")
                    binding.txtResult.text = "fail to recognise!!"
                }
            }

            override fun onFailure(call:Call<ResponseBody>, t: Throwable) {
                // 处理网络请求失败的情况
                Log.e(TAG, "网络请求失败", t)
                binding.txtResult.text = "fail to recognise!"
                binding.txtmessage.visibility = View.VISIBLE
                binding.txtmessage.setText(t.message)
            }
        })
//        ResultApi.retrofitService.getPredictResult(body).enqueue(object :
//            Callback<ResponseBody> {
//            override fun onResponse(
//                call: Call<ResponseBody>,
//                response: Response<ResponseBody>
//            ) {
//                //获取服务器响应的数据
////                val json: String = response.body()!!.string()
//                var json: String?
//                try {
//                    val responseBody = response.body()
//                    if (responseBody != null) {
//                        json = responseBody.string()
//                    } else {
//                        // 处理响应体为空的情况，比如记录日志或抛出更具体的异常
//                        throw IOException("Response body is null")
//                    }
//                } catch (e: IOException) {
//                    // 处理IOException，可能是网络问题或其他IO问题
//                    e.printStackTrace()
//                    // 根据需要，你可能想在这里抛出异常，或者进行其他错误处理
//                }
//
//// 接下来，你需要检查json是否为null，然后才能安全地使用它
//                if (json != null) {
//                    // 使用json字符串
//                    var gson = Gson()
//                    var result = gson.fromJson(
//                        json,
//                        Result::class.java
//                    )
//                    // 显示预测结果
//                    binding.txtResult.text = result.prediction
//                } else {
//                    // 处理json为null的情况
//                    throw IOException("Response body is null")
//                    binding.txtResult.text = "fail to recognise!!"
//                }
//
//
//            }
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.d(TAG, "服务器返回失败信息：" + t.message)
//                binding.txtResult.text = "fail to recognise!"
//                binding.txtmessage.visibility = View.VISIBLE
//                binding.txtmessage.setText(t.message); // 设置文本内容
//            }
//        })
    }
}
