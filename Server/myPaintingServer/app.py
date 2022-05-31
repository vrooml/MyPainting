import io
import json

from PIL import Image
from flask import Flask, make_response, Response, send_from_directory, jsonify
from flask import request
from fast_neural_style.neural_style.neural_style import *

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))


class IllegalArgumentError(Exception):
    pass


@app.route('/processPicture', methods=['POST'])
def handle_picture():
    try:
        if request.files.get("image") is not None and request.headers.get("style") is not None:
            picture = request.files.get("image")
            model_path = request.headers.get("style")
            file_name = picture.filename
        else:
            raise IllegalArgumentError

        input_path = basedir+"/fast_neural_style/input/" + file_name
        output_path = basedir+"/fast_neural_style/output/" + file_name
        model_path = basedir+"/fast_neural_style/saved_models/"+model_path+".pth"
        picture.save(input_path)
        process(input_path, output_path, model_path)
        image_data = open(output_path, 'rb').read()
        res = make_response(image_data)
        # res = {'code': 200,
        #        'msg': "success",
        #        'data': image_data}
        res.headers['Content-Type'] = 'image/png'
        return res
    except IllegalArgumentError as e:
        return "Bad Request:Illegal argument", 400
    # except OSError as e:
    #     return "Internal Error", 500


if __name__ == '__main__':
    app.run()
