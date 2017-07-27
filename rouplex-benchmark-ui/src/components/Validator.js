var validator = module.exports = {};

validator.isNumeric = function(value) {
  return !isNaN(parseFloat(value)) && isFinite(value);
}

validator.isUndefinedNullOrEmpty = function(value) {
  return typeof value === 'undefined' || value == null || value == "";
}

validator.validateIntValueWithinRange = function (value, range, params) {
  if (!validator.isUndefinedNullOrEmpty(value)) {
    if (!validator.isNumeric(value)) {
      // little workaround for partial failures when typing negative or fractional numbers
      if (!params.validateSubmittable) {
        value += "1";
      }

      if (!validator.isNumeric(value)) {
        return "error";
      }

      value = parseInt(value);
    }

    if (range && (value < range.min || value >= range.max)) {
      return "error";
    }
  }
  else if (params.validateSubmittable && !range.optional) {
    return "error";
  }
  else if (!params.omitSuccessEffect) {
    return "success";
  }
};

validator.validateIntRangeWithinRange = function (value, range, params) {
  var check = validator.validateIntValueWithinRange(value.min, range, params);
  if (check && check != "success") {
    return check;
  }

  check = validator.validateIntValueWithinRange(value.max, range, params);
  if (check && check != "success") {
    return check;
  }

  if (typeof value.min !== 'undefined' && typeof value.max !== 'undefined' &&
    !params.allowSuccessorEqualOrLesser && parseInt(value.min) >= parseInt(value.max)) {
    return "error";
  }

  if (!params.omitSuccessEffect) {
    return "success";
  }
};
