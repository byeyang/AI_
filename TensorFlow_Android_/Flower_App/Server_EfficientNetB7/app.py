from flask import Flask, jsonify, request
import io
import base64
import numpy as np
from PIL import Image
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import img_to_array

app = Flask('__name__')  # 初始化App
app.config['JSON_AS_ASCII'] = False
model_flower = load_model('models\EfficientNetB7')  # 加载花朵识别模型


@app.route('/')  # 根目录服务端点
def index():
    message = {'Welcome': '欢迎来到智能化应用服务器！'}
    return jsonify(message), 200


# 对104种花朵预测识别的API
CLASSES = ['pink primrose', 'hard-leaved pocket orchid', 'canterbury bells', 'sweet pea',
           'wild geranium', 'tiger lily', 'moon orchid', 'bird of paradise', 'monkshood',
           'globe thistle',  # 00 - 09
           'snapdragon', "colt's foot", 'king protea', 'spear thistle', 'yellow iris', 'globe-flower',
           'purple coneflower', 'peruvian lily', 'balloon flower', 'giant white arum lily',  # 10 - 19
           'fire lily', 'pincushion flower', 'fritillary', 'red ginger', 'grape hyacinth', 'corn poppy',
           'prince of wales feathers', 'stemless gentian', 'artichoke', 'sweet william',  # 20 - 29
           'carnation', 'garden phlox', 'love in the mist', 'cosmos', 'alpine sea holly',
           'ruby-lipped cattleya', 'cape flower', 'great masterwort', 'siam tulip', 'lenten rose',  # 30 - 39
           'barberton daisy', 'daffodil', 'sword lily', 'poinsettia', 'bolero deep blue', 'wallflower',
           'marigold', 'buttercup', 'daisy', 'common dandelion',  # 40 - 49
           'petunia', 'wild pansy', 'primula', 'sunflower', 'lilac hibiscus', 'bishop of llandaff', 'gaura',
           'geranium', 'orange dahlia', 'pink-yellow dahlia',  # 50 - 59
           'cautleya spicata', 'japanese anemone', 'black-eyed susan', 'silverbush', 'californian poppy',
           'osteospermum', 'spring crocus', 'iris', 'windflower', 'tree poppy',  # 60 - 69
           'gazania', 'azalea', 'water lily', 'rose', 'thorn apple', 'morning glory', 'passion flower', 'lotus',
           'toad lily', 'anthurium',  # 70 - 79
           'frangipani', 'clematis', 'hibiscus', 'columbine', 'desert-rose', 'tree mallow', 'magnolia',
           'cyclamen ', 'watercress', 'canna lily',  # 80 - 89
           'hippeastrum ', 'bee balm', 'pink quill', 'foxglove', 'bougainvillea', 'camellia', 'mallow',
           'mexican petunia', 'bromelia', 'blanket flower',  # 90 - 99
           'trumpet creeper', 'blackberry lily', 'common tulip', 'wild rose']  # 100 - 103


# 图像预处理
def preprocess_image_flower(image, target_size):
    image = image.resize(target_size)
    image = img_to_array(image)
    image = np.expand_dims(image, axis=0)

    return image


# 104种花朵预测 API
@app.route('/predict_flower', methods=['post'])
def predict_flower():
    message = request.get_json(force=True)  # 接收客户机json数据
    image = message['image']  # 提取图像json数据
    decode_image = base64.b64decode(image)  # 解码
    image = Image.open(io.BytesIO(decode_image))  # 还原为图像
    # 图像预处理，升维，缩放，转为模型需要的格式
    processed_image = preprocess_image_flower(image, target_size=(512, 512))
    prediction = model_flower.predict(processed_image)[0]  # 预测
    index = np.argmax(prediction)  # 最大概率的索引
    flower_name = CLASSES[index]  # 花朵名称
    response = {  # 返回结果的json串
        'prediction': flower_name
    }
    return jsonify(response), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)  # 启动服务器