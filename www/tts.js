/*

    Cordova Text-to-Speech Plugin
    https://github.com/vilic/cordova-plugin-tts

    by VILIC VANE
    https://github.com/vilic

    MIT License

*/

exports.speak = function (text) {
    return new Promise(function (resolve, reject) {
        var options = {};

        if (typeof text == "string") {
            options.text = text;
        } else {
            options = text;
        }

        cordova.exec(resolve, reject, "TTS", "speak", [options]);
    });
};

exports.stop = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "stop", []);
    });
};

exports.checkLanguage = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "checkLanguage", []);
    });
};

exports.getVoices = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "getVoices", []);
    });
};

exports.openInstallTts = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "openInstallTts", []);
    });
};

exports.registerSynthesisCallback = () => {
    return new Promise((resolve, reject) => {
        cordova.exec(resolve, reject, "TTS", "registerSynthesisCallback", []);
    });
};

// exports.registerRangeStartCallback = () => {
//     return new Promise((resolve, reject) => {
//         cordova.exec(resolve, reject, "TTS", "registerRangeStartCallback", []);
//     });
// };

exports.registerRangeStartCallback = (printFunc) => {
    printFunc();

    cordova.exec(
        function (data) {
            printFunc(data);
        },
        null,
        "TTS",
        "registerRangeStartCallback",
        []
    );
};
