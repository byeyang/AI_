from flask import Flask, jsonify, request
import io
import base64
import numpy as np
from PIL import Image
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import img_to_array

app = Flask('__name__')  # 初始化App
app.config['JSON_AS_ASCII'] = False
model_flower = load_model('models/Flower_Types_Five.h5')  # 加载花朵识别模型
@app.route('/')  # 根目录服务端点
def index():
    message = {'Welcome': '欢迎来到智能化应用服务器！'}
    return jsonify(message), 200
# 对104种花朵预测识别的API
CLASSES2 =['Lilly', 'Lotus', 'Orchid', 'Sunflower', 'Tulip']

# 图像预处理
def preprocess_image_flower(image, target_size):
    image = image.resize(target_size)
    image = img_to_array(image)
    image = np.expand_dims(image, axis=0)
    # img = image.resize((224, 224))
    # img_array = tf.keras.preprocessing.image.img_to_array(img)
    # img_array = tf.expand_dims(img_array, 0)
    return image
# 104种花朵预测 API
@app.route('/predict_flower', methods=['post'])
def predict_flower():
    print("jjjjj")
    message = request.get_json(force=True)  # 接收客户机json数据
    image = message['image']  # 提取图像json数据
    decode_image = base64.b64decode(image)  # 解码
    image = Image.open(io.BytesIO(decode_image)) # 还原为图像
    # 图像预处理，升维，缩放，转为模型需要的格式
    # processed_image = preprocess_image_flower(image, target_size=(512, 512))
    processed_image = preprocess_image_flower(image, target_size=(224, 224))
    # predictions = loaded_model.predict(img_array)
    # class_labels = classes
    # score = tf.nn.softmax(predictions[0])
    # print(f"{class_labels[tf.argmax(score)]}")

    prediction = model_flower.predict(processed_image)[0]  # 预测
    index = np.argmax(prediction) # 最大概率的索引
    flower_name = CLASSES2[index]  # 花朵名称
    response = {  # 返回结果的json串
        'prediction': flower_name
    }
    return jsonify(response), 200



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True) # 启动服务器
